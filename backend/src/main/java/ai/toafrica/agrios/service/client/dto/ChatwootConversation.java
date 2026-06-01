package ai.toafrica.agrios.service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Slim view of a Chatwoot Conversation as seen by AgriOS.
 *
 * <p>Sprint 41 — we use this in the conversation list / detail screens
 * inside the AgriOS-native Customer Service UI. Unknown fields are ignored
 * so the DTO survives Chatwoot API additions across versions.</p>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatwootConversation {
    private Long id;
    /** Per-account display id, what the URL bar shows: /conversations/{display_id}. */
    private Long displayId;
    /** open / resolved / pending / snoozed */
    private String status;
    /** "Channel::Email" / "Channel::WebWidget" / "Channel::Whatsapp" / etc. */
    private String channel;
    private Long inboxId;
    private Long contactInbox;
    /** Last activity timestamp (epoch seconds). */
    private Long lastActivityAt;
    private Long createdAt;
    private Long updatedAt;
    /** Counter of unread incoming messages. */
    private Integer unreadCount;
    /** Sender (customer) — minimal projection only. */
    private ChatwootContact meta;
    /** Nested contact if Chatwoot returns it under a different key in newer versions. */
    private ChatwootContact contact;
    /** Most recent message (for list previews). */
    private List<ChatwootMessageEnvelope> messages;
    private Long assigneeId;

    /** Slim envelope for nested-message previews returned in list payloads. */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChatwootMessageEnvelope {
        private Long id;
        private String content;
        /** 0 = incoming (from customer), 1 = outgoing (from agent / bot). */
        private Integer messageType;
        private Boolean privateNote;
        private Long createdAt;
    }
}
