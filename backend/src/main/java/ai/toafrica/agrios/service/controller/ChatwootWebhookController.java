package ai.toafrica.agrios.service.controller;

import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.service.config.ChatwootProperties;
import ai.toafrica.agrios.service.entity.CsContactLink;
import ai.toafrica.agrios.service.entity.CsEventLog;
import ai.toafrica.agrios.service.mapper.CsContactLinkMapper;
import ai.toafrica.agrios.service.mapper.CsEventLogMapper;
import ai.toafrica.agrios.service.service.AiAgentService;
import ai.toafrica.agrios.service.service.CsEventLogger;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Webhook receiver for Chatwoot push events.
 *
 * <p>Sprint 40d v0.1 scope:</p>
 * <ul>
 *   <li>Public endpoint (no JWT) — Chatwoot has no AgriOS credentials</li>
 *   <li>Optional HMAC verification via shared secret (recommended for prod)</li>
 *   <li>Resolve the AgriOS Customer.id from the inbound payload's contact</li>
 *   <li>Write one row to {@code service_event_log} with an idempotency key
 *       derived from {@code event_type + message_id|conversation_id}</li>
 * </ul>
 *
 * <p>This receiver is intentionally dumb — it stores and acknowledges. The
 * actual business reactions (auto-reply, ticket creation, escalation) come
 * in Sprint 41 once we have AI Agent traffic to learn from.</p>
 *
 * <p>Chatwoot configures the URL in Settings → Integrations → Webhooks.
 * Subscribe to: {@code conversation_created}, {@code conversation_updated},
 * {@code conversation_status_changed}, {@code message_created},
 * {@code message_updated}, {@code contact_created}, {@code contact_updated}.</p>
 */
@Slf4j
@Tag(name = "91 · Service - Webhook", description = "Inbound from Chatwoot")
@RestController
@RequestMapping({"/v1/cs/webhook", "/v1/service/webhook"})
@RequiredArgsConstructor
public class ChatwootWebhookController {

    private final CsEventLogger eventLogger;
    private final CsContactLinkMapper linkMapper;
    private final CsEventLogMapper logMapper;
    private final ObjectMapper json;
    private final ChatwootProperties props;
    private final AiAgentService aiAgent;

    /**
     * Liveness probe — used by Chatwoot's webhook UI's "test" button and by
     * monitoring. Returns 200 unconditionally; no auth, no side effects.
     */
    @Operation(summary = "Webhook reachability probe")
    @GetMapping("/chatwoot")
    public R<Map<String, Object>> ping() {
        return R.ok(Map.of("ok", true, "service", "agrios", "endpoint", "chatwoot-webhook"));
    }

    /**
     * Main webhook entry. Chatwoot POSTs JSON here for every subscribed event.
     *
     * <p>We always return HTTP 200 — Chatwoot retries on non-2xx, which can
     * snowball if we accidentally throw. Errors are logged, not raised.</p>
     *
     * <p>Optional signature header: {@code X-Chatwoot-Signature}. If
     * {@link ChatwootProperties} carries a webhook secret (not yet exposed —
     * placeholder for Sprint 41), we verify HMAC-SHA256 of the raw body.</p>
     */
    @Operation(summary = "Chatwoot webhook receiver (any event_type)")
    @PostMapping(value = "/chatwoot", consumes = MediaType.APPLICATION_JSON_VALUE)
    public R<Map<String, Object>> receive(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Chatwoot-Signature", required = false) String signature,
            HttpServletRequest req) {

        // Always log a "received" line at INFO so an ops person can confirm
        // the integration is alive even when nothing makes it into the DB.
        log.info("[Chatwoot webhook] from={} bytes={} sig={}",
                req.getRemoteAddr(), rawBody == null ? 0 : rawBody.length(),
                signature == null ? "none" : "present");

        if (!verifySignatureIfConfigured(rawBody, signature)) {
            log.warn("[Chatwoot webhook] signature mismatch — payload ignored");
            return R.ok(Map.of("status", "ignored", "reason", "signature_mismatch"));
        }

        try {
            JsonNode root = json.readTree(rawBody == null ? "{}" : rawBody);
            String event = root.path("event").asText("unknown");

            // Compose an idempotency key so a redelivered webhook only logs once.
            String idemKey = buildIdempotencyKey(event, root);
            if (idemKey != null && logMapper.selectCount(
                    new LambdaQueryWrapper<CsEventLog>()
                            .eq(CsEventLog::getIdempotencyKey, idemKey)
            ) > 0) {
                log.info("[Chatwoot webhook] duplicate event {} key={} — skipped", event, idemKey);
                return R.ok(Map.of("status", "duplicate"));
            }

            ResolvedTarget target = resolveAgriosTarget(root);

            // Build a single audit row covering every event_type. Body is kept
            // intact so Sprint 41 rule code can re-read whatever it needs.
            CsEventLog logRow = new CsEventLog();
            logRow.setEventType("webhook." + event);
            logRow.setDirection("inbound");
            logRow.setSubjectType(target.entityType);
            logRow.setSubjectId(target.entityId);
            logRow.setChatwootAccountId(longOrNull(root, "account", "id"));
            logRow.setChatwootConversationId(extractConversationId(root, event));
            logRow.setChatwootMessageId(extractMessageId(root, event));
            logRow.setPayload(rawBody);
            logRow.setResult("ok");
            logRow.setIdempotencyKey(idemKey);
            logMapper.insert(logRow);

            log.info("[Chatwoot webhook] persisted event={} conversation={} agriosEntity={}/{}",
                    event, logRow.getChatwootConversationId(),
                    target.entityType, target.entityId);

            // Sprint 40f: dispatch new customer messages to the AI Agent.
            // Async — the agent will call back into Chatwoot on its own time.
            if ("message_created".equals(event)) {
                aiAgent.onMessageCreated(root);
            }

            return R.ok(Map.of(
                    "status", "ok",
                    "event", event,
                    "logId", logRow.getId()
            ));

        } catch (Exception e) {
            // Never propagate — Chatwoot retries on non-2xx and we'd build a
            // backlog. Persist a failure row and acknowledge.
            log.error("[Chatwoot webhook] processing failed: {}", e.getMessage(), e);
            eventLogger.failed("webhook.unprocessed", "inbound", null, null, rawBody, e.getMessage());
            return R.ok(Map.of("status", "logged_failure"));
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * If a webhook shared secret is configured, verify {@code X-Chatwoot-Signature}
     * against an HMAC-SHA256 of the raw body. Returns true if the configuration
     * is empty (verification skipped) or if the signature matches.
     *
     * <p>Sprint 40d v0.1: the secret is read from
     * {@link ChatwootProperties#getWebhookSecret()} (added in a follow-up — for
     * now we let any payload through, since dev is on a localhost network).</p>
     */
    private boolean verifySignatureIfConfigured(String body, String signature) {
        String secret = props.getWebhookSecret();
        if (secret == null || secret.isBlank()) return true; // verification off
        if (signature == null || signature.isBlank()) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((body == null ? "" : body).getBytes(StandardCharsets.UTF_8));
            String expected = bytesToHex(digest);
            return constantTimeEquals(expected, signature.trim());
        } catch (Exception e) {
            log.error("[Chatwoot webhook] HMAC verify failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Walk the payload looking for a Chatwoot contact id, then resolve back to
     * the AgriOS Customer via {@code service_contact_link}. Returns NULLs if we
     * can't trace it back — that just means an unlinked contact, fine for v0.1.
     */
    private ResolvedTarget resolveAgriosTarget(JsonNode root) {
        Long contactId = extractContactId(root);
        if (contactId == null) return ResolvedTarget.EMPTY;
        CsContactLink link = linkMapper.selectOne(
                new LambdaQueryWrapper<CsContactLink>()
                        .eq(CsContactLink::getChatwootContactId, contactId)
                        .last("LIMIT 1")
        );
        if (link == null) return ResolvedTarget.EMPTY;
        return new ResolvedTarget(link.getSubjectType(), link.getSubjectId());
    }

    /**
     * Chatwoot puts the contact id in different places depending on event:
     *   conversation.* and message.*: root.contact.id  (sometimes root.sender.id)
     *   contact.*:                    root.id
     */
    private Long extractContactId(JsonNode root) {
        Long v = longOrNull(root, "contact", "id");
        if (v != null) return v;
        v = longOrNull(root, "sender", "id");
        if (v != null) return v;
        // For contact_created / contact_updated, the contact is the root.
        if (root.has("id") && "contact_created".equals(root.path("event").asText())
                || "contact_updated".equals(root.path("event").asText())) {
            return longOrNull(root, "id");
        }
        // conversation.contact_inbox.contact_id (some versions)
        v = longOrNull(root, "conversation", "contact_inbox", "contact_id");
        return v;
    }

    private Long extractConversationId(JsonNode root, String event) {
        if (event.startsWith("conversation_")) return longOrNull(root, "id");
        if (event.startsWith("message_")) return longOrNull(root, "conversation", "id");
        return null;
    }

    private Long extractMessageId(JsonNode root, String event) {
        if (event.startsWith("message_")) return longOrNull(root, "id");
        return null;
    }

    private String buildIdempotencyKey(String event, JsonNode root) {
        if (event == null || event.isBlank()) return null;
        Long primary;
        if (event.startsWith("message_")) {
            primary = longOrNull(root, "id");
            return "cw." + event + ".msg." + primary + "." + root.path("updated_at").asText("");
        }
        if (event.startsWith("conversation_")) {
            primary = longOrNull(root, "id");
            return "cw." + event + ".conv." + primary + "." + root.path("updated_at").asText("");
        }
        if (event.startsWith("contact_")) {
            primary = longOrNull(root, "id");
            return "cw." + event + ".contact." + primary;
        }
        return "cw." + event + "." + root.path("created_at").asText("");
    }

    /** Walk nested JSON paths safely, returning null if any node is missing. */
    private static Long longOrNull(JsonNode node, String... path) {
        JsonNode cur = node;
        for (String p : path) {
            if (cur == null || cur.isMissingNode()) return null;
            cur = cur.path(p);
        }
        if (cur == null || cur.isMissingNode() || cur.isNull()) return null;
        if (!cur.canConvertToLong()) return null;
        return cur.asLong();
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    /** Constant-time comparison so HMAC checks don't leak the secret via timing. */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) diff |= a.charAt(i) ^ b.charAt(i);
        return diff == 0;
    }

    /** Tiny tuple for resolveAgriosTarget. */
    private record ResolvedTarget(String entityType, Long entityId) {
        static final ResolvedTarget EMPTY = new ResolvedTarget(null, null);
    }
}
