package ai.toafrica.agrios.service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Africa's Talking SMS gateway configuration.
 *
 * <p>Sprint 43 v0.1 — single global account per AgriOS install. Most Kenyan
 * farms run one Africa's Talking number for customer SMS, so storing creds
 * in env is enough. Future multi-tenant deployments can move this to a
 * per-customer table; the API surface stays the same.</p>
 *
 * <p>Configure via env vars:</p>
 * <pre>
 *   AFRICASTALKING_USERNAME       — "sandbox" for the dev sandbox, otherwise your AT username
 *   AFRICASTALKING_API_KEY        — long string from AT dashboard → API key
 *   AFRICASTALKING_SENDER_ID      — short code / alphanumeric sender, optional
 *   AFRICASTALKING_SANDBOX        — true to hit the sandbox URL (no real money / no real delivery)
 *   AFRICASTALKING_SMS_INBOX_ID   — Chatwoot inbox id created by the SMS setup wizard
 * </pre>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "agrios.africastalking")
public class AfricasTalkingProperties {

    private String username = "";
    private String apiKey = "";
    private String senderId = "";
    private boolean sandbox = true;

    /**
     * The single Chatwoot inbox id that represents the Africa's Talking SMS
     * channel for this install. Webhook traffic referencing this inbox is
     * routed through the SMS bridge.
     */
    private Long smsInboxId;

    /** Returns the correct base URL (sandbox or live). */
    public String getMessagingUrl() {
        return sandbox
                ? "https://api.sandbox.africastalking.com/version1/messaging"
                : "https://api.africastalking.com/version1/messaging";
    }

    /** Cheap check used by the SMS bridge to short-circuit when not configured. */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank()
            && username != null && !username.isBlank();
    }
}
