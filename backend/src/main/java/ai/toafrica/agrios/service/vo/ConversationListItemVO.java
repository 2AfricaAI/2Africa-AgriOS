package ai.toafrica.agrios.service.vo;

import ai.toafrica.agrios.service.client.dto.ChatwootConversation;
import lombok.Builder;
import lombok.Data;

/**
 * Slim row used by the conversation list page. We project just what the UI
 * needs so we can change the underlying Chatwoot shape without churning the
 * frontend.
 */
@Data
@Builder
public class ConversationListItemVO {

    private Long id;
    private Long displayId;
    /** open / resolved / pending / snoozed */
    private String status;
    /** "Channel::Email" / "Channel::Whatsapp" / ... — the UI maps to icons. */
    private String channel;
    private Long inboxId;

    /** Resolved AgriOS Customer.id via service_contact_link (null if unlinked). */
    private Long agriosCustomerId;
    /** Resolved AgriOS Customer.code (e.g. CUS-00042) — for at-a-glance ID. */
    private String agriosCustomerCode;

    private String contactName;
    private String contactPhone;
    private String contactEmail;

    private String lastMessagePreview;
    private Long lastActivityAt;
    private Integer unreadCount;
    private Long assigneeId;

    public static ConversationListItemVO from(ChatwootConversation c) {
        var b = ConversationListItemVO.builder()
                .id(c.getId())
                .displayId(c.getDisplayId())
                .status(c.getStatus())
                .channel(c.getChannel())
                .inboxId(c.getInboxId())
                .lastActivityAt(c.getLastActivityAt())
                .unreadCount(c.getUnreadCount())
                .assigneeId(c.getAssigneeId());
        if (c.getContact() != null) {
            b.contactName(c.getContact().getName())
             .contactPhone(c.getContact().getPhoneNumber())
             .contactEmail(c.getContact().getEmail());
        } else if (c.getMeta() != null) {
            b.contactName(c.getMeta().getName())
             .contactPhone(c.getMeta().getPhoneNumber())
             .contactEmail(c.getMeta().getEmail());
        }
        if (c.getMessages() != null && !c.getMessages().isEmpty()) {
            ChatwootConversation.ChatwootMessageEnvelope last = c.getMessages().get(0);
            b.lastMessagePreview(last.getContent());
        }
        return b.build();
    }
}
