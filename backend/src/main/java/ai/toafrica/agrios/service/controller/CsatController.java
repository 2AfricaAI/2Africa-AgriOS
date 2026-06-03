package ai.toafrica.agrios.service.controller;

import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.framework.security.SecurityUtil;
import ai.toafrica.agrios.service.entity.CsCsatResponse;
import ai.toafrica.agrios.service.service.CsatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Sprint 50d -- CSAT (Customer Satisfaction) endpoints.
 *
 * <p>Split into two surfaces:</p>
 * <ul>
 *   <li>Agent-side: {@code /v1/cs/csat/link} -- generate a survey link.
 *       Requires {@code cs:csat:send} permission.</li>
 *   <li>Customer-side: {@code /v1/cs/csat/public/**} -- token-based,
 *       no JWT required (permitAll in SecurityConfig).</li>
 * </ul>
 */
@Slf4j
@Tag(name = "93 - CS - CSAT", description = "Customer satisfaction survey")
@RestController
@RequestMapping({"/v1/cs/csat", "/v1/service/csat"})
@RequiredArgsConstructor
public class CsatController {

    private final CsatService csatService;

    // -------- agent-facing (JWT + perm) --------

    @Operation(summary = "Generate or reuse a CSAT survey link for a conversation")
    @PostMapping("/link")
    @PreAuthorize("hasAuthority('cs:csat:send')")
    public R<Map<String, Object>> generateLink(@RequestBody LinkRequest body) {
        Long userId = SecurityUtil.currentUserId();
        CsatService.CsatLink link = csatService.generateLink(body.getConversationId(), userId);
        Map<String, Object> out = new HashMap<>();
        out.put("token", link.token());
        out.put("url", link.url());
        out.put("expiresAt", link.expiresAt());
        return R.ok(out);
    }

    // -------- customer-facing (token-only, no JWT) --------

    @Operation(summary = "Validate a CSAT token and return survey metadata")
    @GetMapping("/public/{token}")
    public R<Map<String, Object>> probe(@PathVariable String token) {
        CsCsatResponse row = csatService.loadByToken(token);
        Map<String, Object> out = new HashMap<>();
        out.put("token", row.getToken());
        out.put("conversationId", row.getChatwootConversationId());
        out.put("expiresAt", row.getExpiresAt());
        return R.ok(out);
    }

    @Operation(summary = "Submit a CSAT rating + optional comment")
    @PostMapping("/public/{token}")
    public R<Map<String, Object>> submit(@PathVariable String token,
                                         @RequestBody SubmitRequest body) {
        CsCsatResponse row = csatService.submit(token,
                body.getRating() == null ? 0 : body.getRating(),
                body.getComment());
        Map<String, Object> out = new HashMap<>();
        out.put("rating", row.getRating());
        out.put("submittedAt", row.getSubmittedAt());
        return R.ok(out);
    }

    @Data
    public static class LinkRequest {
        private Long conversationId;
    }

    @Data
    public static class SubmitRequest {
        private Integer rating;
        private String comment;
    }
}
