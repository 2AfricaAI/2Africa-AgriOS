package ai.toafrica.agrios.service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * WhatsApp zero-cost policy (Sprint 47).
 *
 * <p>Meta charges per business-initiated WhatsApp conversation. Inside the
 * 24 hour "service window" after a customer-initiated message, business
 * replies are FREE. AgriOS enforces this window so small/medium farms
 * never accidentally rack up Meta charges.</p>
 *
 * <p>Policy summary:</p>
 * <ul>
 *   <li>Customer message arrives → service window opens (free for 24h)</li>
 *   <li>Within window: agent public reply allowed (free)</li>
 *   <li>Outside window: agent public reply BLOCKED (would require paid template)</li>
 *   <li>Private internal notes: always allowed (never sent to customer, zero cost)</li>
 * </ul>
 *
 * <p>Configure via env (backend/.env):</p>
 * <pre>
 *   WHATSAPP_SERVICE_WINDOW_HOURS=24
 *   WHATSAPP_SIMULATED_INBOX_IDS=2,3   # CSV of Chatwoot inbox ids to treat
 *                                       # as WhatsApp for policy purposes,
 *                                       # used for local testing with the
 *                                       # Chatwoot API channel (no real Meta)
 * </pre>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "agrios.whatsapp")
public class WhatsAppPolicyProperties {

    /** Service window length in hours. Meta's official policy is 24h. */
    private int serviceWindowHours = 24;

    /**
     * Inbox IDs that should be treated as WhatsApp for policy enforcement
     * even when their Chatwoot channel_type is not "Channel::Whatsapp".
     *
     * <p>Use case: local end-to-end testing without Meta WhatsApp Cloud.
     * Create an "API channel" inbox in Chatwoot and put its id here.
     * The reply guard and UI countdown will fire as if it were a real
     * WhatsApp inbox.</p>
     */
    private Set<Long> simulatedInboxIds = Collections.emptySet();

    /**
     * Bind a comma-separated string from env to the simulatedInboxIds set.
     * Spring's relaxed binding handles "1,2,3" for Set&lt;Long&gt; natively,
     * but we accept blanks / whitespace defensively here.
     */
    public void setSimulatedInboxIds(String raw) {
        if (raw == null || raw.isBlank()) {
            this.simulatedInboxIds = Collections.emptySet();
            return;
        }
        Set<Long> ids = new LinkedHashSet<>();
        for (String token : raw.split(",")) {
            String t = token.trim();
            if (t.isEmpty()) continue;
            try {
                ids.add(Long.parseLong(t));
            } catch (NumberFormatException ignored) {
                // skip malformed entries; do not crash app startup
            }
        }
        this.simulatedInboxIds = ids;
    }

    /** Convenience: returns service window length in milliseconds. */
    public long getServiceWindowMillis() {
        return serviceWindowHours * 3_600_000L;
    }
}
