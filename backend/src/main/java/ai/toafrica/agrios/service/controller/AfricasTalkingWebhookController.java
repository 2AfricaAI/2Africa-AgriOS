package ai.toafrica.agrios.service.controller;

import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.service.client.AfricasTalkingClient;
import ai.toafrica.agrios.service.config.AfricasTalkingProperties;
import ai.toafrica.agrios.service.config.ChatwootProperties;
import ai.toafrica.agrios.service.service.ServiceEventLogger;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * SMS bridge between Chatwoot and Africa's Talking.
 *
 * <p>Two endpoints:</p>
 * <ul>
 *   <li>{@code /outbound} — Chatwoot POSTs here when an agent replies in the
 *       SMS inbox. We pull out the recipient phone + body and call Africa's
 *       Talking to actually send the SMS.</li>
 *   <li>{@code /incoming} — Africa's Talking POSTs here when a customer
 *       texts the AT short code. We forward the message into the Chatwoot
 *       SMS inbox via its public API so the agent sees a new conversation.</li>
 * </ul>
 *
 * <p>Both endpoints are public (no JWT) — they are HMAC-free in v0.1, since
 * AT does not sign callbacks. Sprint 44 will add allow-listing by source IP
 * and an optional shared secret in the URL path.</p>
 */
@Slf4j
@Tag(name = "93 · Service - SMS (Africa's Talking)", description = "SMS bridge for Chatwoot")
@RestController
@RequestMapping("/v1/service/webhook/africastalking")
@RequiredArgsConstructor
public class AfricasTalkingWebhookController {

    private final AfricasTalkingProperties atProps;
    private final ChatwootProperties cwProps;
    private final AfricasTalkingClient at;
    private final ServiceEventLogger eventLogger;
    private final ObjectMapper json = new ObjectMapper();

    // -----------------------------------------------------------------------
    // OUTBOUND  Chatwoot agent reply  →  Africa's Talking SMS to customer
    // -----------------------------------------------------------------------

    @Operation(summary = "Chatwoot API Channel webhook — relay outbound agent reply to Africa's Talking")
    @PostMapping(value = "/outbound", consumes = MediaType.APPLICATION_JSON_VALUE)
    public R<Map<String, Object>> outbound(@RequestBody String rawBody) {
        log.info("[AT-SMS outbound] received {} bytes", rawBody == null ? 0 : rawBody.length());

        if (!atProps.isConfigured()) {
            log.warn("[AT-SMS outbound] AT not configured, ignoring");
            return R.ok(Map.of("status", "skipped_unconfigured"));
        }

        try {
            JsonNode root = json.readTree(rawBody == null ? "{}" : rawBody);

            // Chatwoot API Channel payload shape (message_created):
            //   message_type = "outgoing" / "incoming"
            //   content      = message text
            //   conversation.contact_inbox.source_id = customer phone (E.164)
            //   conversation.meta.sender.phone_number = same phone
            String messageType = root.path("message_type").asText("");
            if (!"outgoing".equals(messageType)) {
                log.debug("[AT-SMS outbound] message_type={} — only outgoing is relayed", messageType);
                return R.ok(Map.of("status", "ignored_non_outgoing"));
            }
            if (root.path("private").asBoolean(false)) {
                return R.ok(Map.of("status", "ignored_private_note"));
            }

            String content = root.path("content").asText("");
            String to = extractRecipient(root);

            if (to == null || to.isBlank() || content == null || content.isBlank()) {
                log.warn("[AT-SMS outbound] missing recipient or content; raw={}", truncate(rawBody, 200));
                return R.ok(Map.of("status", "missing_fields"));
            }

            List<AfricasTalkingClient.Recipient> result = at.sendSms(List.of(to), content);

            eventLogger.ok("at.sms.sent", "outbound", null, null,
                    Map.of("to", to, "preview", preview(content), "recipients", result));
            return R.ok(Map.of(
                    "status", "sent",
                    "recipients", result
            ));

        } catch (Exception e) {
            log.error("[AT-SMS outbound] failed: {}", e.getMessage(), e);
            eventLogger.failed("at.sms.sent", "outbound", null, null, rawBody, e.getMessage());
            return R.ok(Map.of("status", "error", "error", e.getMessage()));
        }
    }

    /**
     * Walk the Chatwoot payload looking for a recipient phone number. The
     * SMS inbox stores it as the contact's {@code source_id} (set when the
     * conversation was created from the inbound SMS), but newer Chatwoot
     * versions also surface it at conversation.meta.sender.phone_number.
     */
    private String extractRecipient(JsonNode root) {
        String v = root.path("conversation").path("contact_inbox").path("source_id").asText("");
        if (!v.isBlank()) return v;
        v = root.path("conversation").path("meta").path("sender").path("phone_number").asText("");
        if (!v.isBlank()) return v;
        v = root.path("sender").path("phone_number").asText("");
        if (!v.isBlank()) return v;
        return null;
    }

    // -----------------------------------------------------------------------
    // INBOUND  Customer SMS  →  Chatwoot conversation in the SMS inbox
    // -----------------------------------------------------------------------

    /**
     * Africa's Talking inbound SMS callback. Configured by the operator in
     * the AT dashboard: Settings → SMS → Callback URLs → "Incoming Messages".
     *
     * <p>AT POSTs form-urlencoded fields: {@code from, to, text, date, id, linkId}.
     * We forward into Chatwoot's API Channel public endpoint so the SMS
     * inbox lights up as if the customer typed directly in Chatwoot.</p>
     */
    @Operation(summary = "Africa's Talking inbound SMS callback — push into Chatwoot SMS inbox")
    @PostMapping(value = "/incoming",
                 consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public R<Map<String, Object>> incoming(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String text,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String linkId,
            @RequestBody(required = false) String rawBody
    ) {
        log.info("[AT-SMS incoming] from={} text-len={} body-bytes={}",
                from, text == null ? 0 : text.length(), rawBody == null ? 0 : rawBody.length());

        Long smsInboxId = atProps.getSmsInboxId();
        if (smsInboxId == null) {
            log.warn("[AT-SMS incoming] no SMS inbox id configured — operator must run setupSms first");
            return R.ok(Map.of("status", "skipped_no_inbox"));
        }
        if (from == null || from.isBlank() || text == null || text.isBlank()) {
            return R.ok(Map.of("status", "missing_fields"));
        }

        try {
            // Step 1 — find the inbox identifier (UUID) Chatwoot uses for its
            // public API. We could cache this, but listInboxes is cheap enough
            // for an inbound trickle.
            String inboxIdentifier = fetchInboxIdentifier(smsInboxId);
            if (inboxIdentifier == null) {
                log.warn("[AT-SMS incoming] inbox {} has no inbox_identifier — is it really an API channel?", smsInboxId);
                return R.ok(Map.of("status", "no_identifier"));
            }

            // Step 2 — create-or-get the contact for this phone number.
            // Chatwoot's public API is upsert on source_id.
            JsonNode contact = upsertPublicContact(inboxIdentifier, from);
            String contactSourceId = contact.path("source_id").asText(from);

            // Step 3 — create (or reuse latest) conversation.
            Long conversationId = createPublicConversation(inboxIdentifier, contactSourceId);

            // Step 4 — post the incoming message into the conversation.
            postPublicMessage(inboxIdentifier, contactSourceId, conversationId, text);

            eventLogger.ok("at.sms.received", "inbound", null, null,
                    Map.of("from", from, "preview", preview(text), "inboxId", smsInboxId,
                           "conversationId", conversationId));
            return R.ok(Map.of("status", "forwarded", "conversationId", conversationId));

        } catch (Exception e) {
            log.error("[AT-SMS incoming] failed: {}", e.getMessage(), e);
            eventLogger.failed("at.sms.received", "inbound", null, null,
                    Map.of("from", from, "text", text), e.getMessage());
            return R.ok(Map.of("status", "error", "error", e.getMessage()));
        }
    }

    // -----------------------------------------------------------------------
    // Chatwoot public API helpers (no api_access_token; uses inbox_identifier)
    // -----------------------------------------------------------------------

    private String fetchInboxIdentifier(Long inboxId) {
        // Reuse the admin token to find the identifier on the inbox row.
        String url = cwProps.getBaseUrl() + "/api/v1/accounts/" + cwProps.getAccountId() + "/inboxes/" + inboxId;
        String body = cwGet(url);
        try {
            JsonNode node = json.readTree(body);
            String id = node.path("inbox_identifier").asText("");
            return id.isBlank() ? null : id;
        } catch (Exception e) {
            return null;
        }
    }

    private JsonNode upsertPublicContact(String inboxIdentifier, String phone) throws Exception {
        String url = cwProps.getBaseUrl() + "/public/api/v1/inboxes/" + inboxIdentifier + "/contacts";
        String payload = "{\"source_id\":\"" + esc(phone) + "\",\"identifier\":\"" + esc(phone)
                       + "\",\"name\":\"SMS " + esc(phone) + "\",\"phone_number\":\"" + esc(phone) + "\"}";
        String body = cwPostJson(url, payload);
        return json.readTree(body);
    }

    private Long createPublicConversation(String inboxIdentifier, String contactSourceId) throws Exception {
        String url = cwProps.getBaseUrl() + "/public/api/v1/inboxes/" + inboxIdentifier
                   + "/contacts/" + URLEncoder.encode(contactSourceId, StandardCharsets.UTF_8) + "/conversations";
        String body = cwPostJson(url, "{}");
        return json.readTree(body).path("id").asLong();
    }

    private void postPublicMessage(String inboxIdentifier, String contactSourceId, Long conversationId, String content)
            throws Exception {
        String url = cwProps.getBaseUrl() + "/public/api/v1/inboxes/" + inboxIdentifier
                   + "/contacts/" + URLEncoder.encode(contactSourceId, StandardCharsets.UTF_8)
                   + "/conversations/" + conversationId + "/messages";
        String payload = "{\"content\":\"" + esc(content) + "\"}";
        cwPostJson(url, payload);
    }

    // -----------------------------------------------------------------------
    // Internals
    // -----------------------------------------------------------------------

    private String cwGet(String url) {
        try (HttpResponse resp = HttpUtil.createRequest(Method.GET, url)
                .header("api_access_token", cwProps.getApiToken())
                .timeout(10_000).execute()) {
            return resp.body();
        }
    }

    private String cwPostJson(String url, String body) {
        try (HttpResponse resp = HttpUtil.createRequest(Method.POST, url)
                .header("Content-Type", "application/json")
                .body(body).timeout(10_000).execute()) {
            return resp.body();
        }
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String preview(String s) {
        if (s == null) return "";
        String oneLine = s.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= 120 ? oneLine : oneLine.substring(0, 117) + "...";
    }
}
