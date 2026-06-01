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
    /** Per-account display id; in Chatwoot v4 this is often equal to id. */
    private Long displayId;
    /** open / resolved / pending / snoozed */
    private String status;
    /** Direct channel string — only present on the single-conversation endpoint. */
    private String channel;
    private Long inboxId;
    private Long contactInbox;
    /** Last activity timestamp (epoch seconds). */
    private Long lastActivityAt;
    private Long createdAt;
    private Long updatedAt;
    /** Counter of unread incoming messages. */
    private Integer unreadCount;
    /**
     * v4 list response stuffs everything (channel + sender + hmac flag) under
     * a {@code meta} object rather than at the root. Detail responses may
     * inline {@code contact} instead, so we keep both fields.
     */
    private Meta meta;
    private ChatwootContact contact;
    /** Most recent message (for list previews). */
    private List<ChatwootMessageEnvelope> messages;
    private Long assigneeId;

    /** Chatwoot v4 conversation-list "meta" wrapper. */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Meta {
        /** "Channel::WebWidget" / "Channel::Email" / "Channel::Whatsapp" / ... */
        private String channel;
        /** The customer-side contact for this conversation. */
        private ChatwootContact sender;
        private Boolean hmacVerified;
    }

    /** Best-effort accessor: returns the contact from whichever shape Chatwoot used. */
    public ChatwootContact resolvedContact() {
        if (contact != null) return contact;
        if (meta != null && meta.getSender() != null) return meta.getSender();
        return null;
    }

    /** Best-effort accessor: channel from root or from meta. */
    public String resolvedChannel() {
        if (channel != null && !channel.isBlank()) return channel;
        if (meta != null && meta.getChannel() != null) return meta.getChannel();
        return null;
    }

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
