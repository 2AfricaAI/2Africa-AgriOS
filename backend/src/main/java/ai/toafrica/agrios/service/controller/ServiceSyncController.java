package ai.toafrica.agrios.service.controller;

import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.service.client.ChatwootClient;
import ai.toafrica.agrios.service.client.LlmClient;
import ai.toafrica.agrios.service.client.LlmRouter;
import ai.toafrica.agrios.service.config.ChatwootProperties;
import ai.toafrica.agrios.service.entity.ServiceContactLink;
import ai.toafrica.agrios.service.service.ContactSyncService;
import ai.toafrica.agrios.service.vo.SyncResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Operator-facing endpoints for the AgriOS / Chatwoot bridge.
 *
 * <p>All endpoints under {@code /v1/service/*} require {@code ROLE_ADMIN} for now —
 * sync is an admin action and the AI Agent/inbox configuration also happens here.
 * Once we add per-agent permissions (Sprint 41+) we will relax this.</p>
 */
@Tag(name = "90 · Service - Sync", description = "AgriOS <-> Chatwoot bridge: contact sync + diagnostics")
@RestController
@RequestMapping("/v1/service")
@RequiredArgsConstructor
public class ServiceSyncController {

    private final ContactSyncService syncService;
    private final ChatwootClient chatwoot;
    private final ChatwootProperties props;
    private final LlmRouter llm;

    @Operation(summary = "Health probe — confirms Chatwoot is reachable and the API token is accepted")
    @GetMapping("/health")
    public R<Map<String, Object>> health() {
        boolean enabled = props.isEnabled();
        boolean reachable = enabled && chatwoot.pingProfile();
        return R.ok(Map.of(
                "enabled", enabled,
                "reachable", reachable,
                "baseUrl", props.getBaseUrl(),
                "accountId", props.getAccountId()
        ));
    }

    // Sprint 40 v0.1: any authenticated user can trigger sync. Sprint 41 will
    // wire this to a fine-grained perm (service:sync:write) once the seed file
    // is updated. Until then, JWT auth filter + Spring Security's default
    // anyRequest().authenticated() is the gate.

    @Operation(summary = "Push one customer to Chatwoot (create or update). Idempotent.")
    @PostMapping("/sync-customer/{id}")
    public R<SyncResultVO> syncCustomer(@PathVariable Long id) {
        ServiceContactLink link = syncService.syncCustomer(id);
        return R.ok(SyncResultVO.from(id, link));
    }

    @Operation(summary = "Bulk push every active customer. Use sparingly — first-time setup.")
    @PostMapping("/sync-customers-all")
    public R<ContactSyncService.BulkResult> syncAllActive() {
        return R.ok(syncService.syncAllActive());
    }

    /**
     * Sprint 40f verification helper: directly hit the configured LLM provider
     * with a single-turn prompt and return the response. Bypasses Chatwoot and
     * the webhook path, so we can confirm Claude / OpenAI plumbing works
     * independently of channel quirks.
     */
    @Operation(summary = "Diagnostic: call the configured LLM with a single prompt")
    @PostMapping("/ai-agent/diagnose")
    public R<Map<String, Object>> diagnoseAiAgent(@RequestBody(required = false) Map<String, String> req) {
        String prompt = (req != null && req.get("prompt") != null && !req.get("prompt").isBlank())
                ? req.get("prompt")
                : "Reply in one short friendly sentence: are you alive?";
        long t0 = System.currentTimeMillis();
        String reply;
        String error = null;
        try {
            reply = llm.complete(List.of(LlmClient.Turn.user(prompt)));
        } catch (Exception e) {
            reply = null;
            error = e.getClass().getSimpleName() + ": " + e.getMessage();
        }
        long ms = System.currentTimeMillis() - t0;
        return R.ok(Map.of(
                "provider", props.getAiAgent().getProvider(),
                "model", props.getAiAgent().getModel(),
                "promptChars", prompt.length(),
                "replyChars", reply == null ? 0 : reply.length(),
                "elapsedMs", ms,
                "reply", reply == null ? "" : reply,
                "error", error == null ? "" : error
        ));
    }
}
