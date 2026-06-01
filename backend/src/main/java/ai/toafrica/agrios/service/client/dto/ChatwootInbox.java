package ai.toafrica.agrios.service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Slim projection of a Chatwoot Inbox. Used by the AgriOS Customer Service
 * UI to populate channel filters and the inbox switcher.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatwootInbox {
    private Long id;
    private String name;
    /** "Channel::Email" / "Channel::WebWidget" / "Channel::Api" / ... */
    private String channelType;
    /** "go@2africa.ai" for email inboxes, phone number for WhatsApp, etc. */
    private String email;
    private String phoneNumber;
    private String avatarUrl;
}
