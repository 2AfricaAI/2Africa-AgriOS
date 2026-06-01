package ai.toafrica.agrios.service.client;

import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.service.config.AfricasTalkingProperties;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Thin REST client for the Africa's Talking SMS API.
 *
 * <p>API doc: https://developers.africastalking.com/docs/sms/sending</p>
 *
 * <p>Payload is form-encoded (NOT JSON). The response is JSON wrapping a
 * SMSMessageData object with per-recipient delivery status.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AfricasTalkingClient {

    private final AfricasTalkingProperties props;
    private final ObjectMapper json = new ObjectMapper();

    /**
     * Send an SMS to one or more recipients. Returns the per-recipient
     * delivery summary. Recipients must be E.164 ({@code +254700123456}).
     */
    public List<Recipient> sendSms(List<String> to, String message) {
        if (!props.isConfigured()) {
            throw new IllegalStateException(
                "Africa's Talking is not configured — set AFRICASTALKING_USERNAME + AFRICASTALKING_API_KEY"
            );
        }
        if (to == null || to.isEmpty()) {
            throw new BusinessException("recipient list is empty");
        }
        if (message == null || message.isBlank()) {
            throw new BusinessException("message is empty");
        }

        StringBuilder body = new StringBuilder();
        body.append("username=").append(enc(props.getUsername()));
        body.append("&to=").append(enc(String.join(",", to)));
        body.append("&message=").append(enc(message));
        if (props.getSenderId() != null && !props.getSenderId().isBlank()) {
            body.append("&from=").append(enc(props.getSenderId()));
        }

        long t0 = System.currentTimeMillis();
        HttpRequest req = HttpUtil.createRequest(Method.POST, props.getMessagingUrl())
                .header("apiKey", props.getApiKey())
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(body.toString())
                .timeout(15_000);

        try (HttpResponse resp = req.execute()) {
            long ms = System.currentTimeMillis() - t0;
            int status = resp.getStatus();
            String text = resp.body();
            if (status < 200 || status >= 300) {
                log.warn("[AT-SMS] HTTP {} in {}ms — {}", status, ms, truncate(text, 300));
                throw new BusinessException("Africa's Talking SMS HTTP " + status + ": " + truncate(text, 300));
            }
            log.info("[AT-SMS] ok in {}ms to={} msgLen={}", ms, to, message.length());
            return parseRecipients(text);
        }
    }

    /**
     * Africa's Talking response shape:
     * <pre>
     * {
     *   "SMSMessageData": {
     *     "Message": "Sent to 1/1 Total Cost: KES 0.8000",
     *     "Recipients": [
     *       { "statusCode": 101, "number": "+254...", "status": "Success", "cost": "KES 0.8000", "messageId": "ATXid_..." }
     *     ]
     *   }
     * }
     * </pre>
     */
    private List<Recipient> parseRecipients(String body) {
        try {
            JsonNode root = json.readTree(body);
            JsonNode recipients = root.path("SMSMessageData").path("Recipients");
            if (!recipients.isArray()) return List.of();
            java.util.List<Recipient> out = new java.util.ArrayList<>(recipients.size());
            for (JsonNode r : recipients) {
                Recipient rec = new Recipient();
                rec.number = r.path("number").asText("");
                rec.status = r.path("status").asText("");
                rec.statusCode = r.path("statusCode").asInt(0);
                rec.cost = r.path("cost").asText("");
                rec.messageId = r.path("messageId").asText("");
                out.add(rec);
            }
            return out;
        } catch (Exception e) {
            log.warn("[AT-SMS] response parse failed: {}", e.getMessage());
            return List.of();
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    /** Per-recipient delivery summary returned by AT. */
    @Data
    public static class Recipient {
        public String number;
        public String status;
        public int statusCode;
        public String cost;
        public String messageId;
    }
}
