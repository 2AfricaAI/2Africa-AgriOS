package ai.toafrica.agrios.service.service;

import ai.toafrica.agrios.service.client.ChatwootClient;
import ai.toafrica.agrios.service.client.dto.ChatwootConversation;
import ai.toafrica.agrios.service.config.WhatsAppPolicyProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Sprint 47 — WhatsApp zero-cost policy enforcement.
 *
 * <p>Decides whether a given conversation is subject to WhatsApp's service-window
 * rules (Meta's "free 24h after customer-initiated inbound" model) and whether
 * an outbound public reply right now would be free.</p>
 *
 * <p>The policy treats a Chatwoot inbox as WhatsApp if either:
 * <ol>
 *   <li>its {@code channel_type} starts with {@code "Channel::Whatsapp"}
 *       (real Meta Cloud / 360dialog / Twilio WhatsApp inboxes), or</li>
 *   <li>its inbox id appears in
 *       {@code agrios.whatsapp.simulated-inbox-ids} (used for local
 *       end-to-end testing with a Chatwoot API channel — no Meta
 *       Cloud account required).</li>
 * </ol>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppPolicyService {

    private final WhatsAppPolicyProperties props;
    private final ChatwootClient chatwoot;

    /** Channel-type prefix Chatwoot uses for all WhatsApp providers. */
    public static final String WHATSAPP_CHANNEL_PREFIX = "Channel::Whatsapp";

    /**
     * Decision returned by {@link #evaluate(ChatwootConversation)}.
     * Always populated so the UI can render a sensible chip even for
     * non-WhatsApp inboxes (where {@code managed} is false).
     */
    public record Decision(
            /* True when this inbox is governed by the WhatsApp policy. */
            boolean managed,
            /* Latest customer-side inbound timestamp (epoch seconds), or null. */
            Long lastInboundAtEpochSec,
            /* Window-close timestamp (epoch seconds), or null when managed=false
             * or when no customer message has ever arrived. */
            Long serviceWindowExpiresAtEpochSec,
            /* True when a public agent reply right now would be free. */
            boolean withinServiceWindow
    ) {}

    /**
     * Returns true when the inbox id appears in the simulated list OR the
     * channel string is a Chatwoot WhatsApp variant.
     */
    public boolean isWhatsAppInbox(String channelType, Long inboxId) {
        if (inboxId != null && props.getSimulatedInboxIds().contains(inboxId)) {
            return true;
        }
        return channelType != null && channelType.startsWith(WHATSAPP_CHANNEL_PREFIX);
    }

    /**
     * Build a policy decision for a fully-loaded conversation. Calls
     * Chatwoot once to fetch message history when the inbox is managed.
     */
    public Decision evaluate(ChatwootConversation c) {
        if (c == null) {
            return new Decision(false, null, null, false);
        }
        boolean managed = isWhatsAppInbox(c.resolvedChannel(), c.getInboxId());
        if (!managed) {
            // Non-WhatsApp inboxes: unrestricted; reply is always allowed.
            return new Decision(false, null, null, true);
        }

        Long lastInbound = findLastInboundAt(c.getId());
        if (lastInbound == null) {
            // WhatsApp inbox but no customer message yet → can't open a
            // service conversation, so no free reply path either.
            return new Decision(true, null, null, false);
        }
        long expires = lastInbound + props.getServiceWindowHours() * 3600L;
        long nowSec = System.currentTimeMillis() / 1000L;
        boolean withinWindow = nowSec < expires;
        return new Decision(true, lastInbound, expires, withinWindow);
    }

    /**
     * Look up the latest customer-side message for a conversation. Returns
     * null when no inbound has ever been received (brand-new outbound-only
     * thread or a freshly created stub).
     *
     * <p>Public so the controller can re-use it without a second Chatwoot
     * round-trip when it already has the messages.</p>
     */
    public Long findLastInboundAt(Long conversationId) {
        if (conversationId == null) return null;
        List<ChatwootClient.ChatMessage> msgs = chatwoot.listMessages(conversationId);
        Long max = null;
        for (ChatwootClient.ChatMessage m : msgs) {
            if (!m.fromCustomer) continue;        // skip agent / bot messages
            if (m.privateNote) continue;          // and internal notes
            if (m.createdAt == null) continue;
            if (max == null || m.createdAt > max) {
                max = m.createdAt;
            }
        }
        return max;
    }
}
