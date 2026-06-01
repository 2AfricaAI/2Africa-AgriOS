package ai.toafrica.agrios.service.vo;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

/**
 * Conversation detail returned to the AgriOS Customer Service UI. Includes
 * the message history plus an AgriOS-side business context block so agents
 * see who they're talking to and the customer's open issues in one place.
 */
@Data
@Builder
public class ConversationDetailVO {

    private Long id;
    private Long displayId;
    private String status;
    private String channel;
    private Long inboxId;
    private Long assigneeId;

    /** Resolved customer info (may be null if the conversation is from a
     *  Chatwoot contact that isn't in service_contact_link yet). */
    private AgriosCustomerSummary customer;

    /** Messages in chronological (oldest → newest) order. */
    @Singular("message")
    private List<MessageVO> messages;

    /** Inline business context for the agent (orders, AR, complaints). */
    private BusinessContext businessContext;

    @Data
    @Builder
    public static class AgriosCustomerSummary {
        private Long id;
        private String code;
        private String name;
        private String type;
        private String contactName;
        private String contactPhone;
        private String creditLevel;
        private String paymentTerms;
        private Integer creditDays;
    }

    @Data
    @Builder
    public static class MessageVO {
        private Long id;
        private String content;
        /** 0 = incoming (customer), 1 = outgoing (agent / bot). */
        private Integer messageType;
        private Boolean privateNote;
        private Long createdAt;
    }

    @Data
    @Builder
    public static class BusinessContext {
        private Integer openOrderCount;
        private Integer overdueArInvoiceCount;
        private java.math.BigDecimal overdueArAmount;
        private Integer openComplaintCount;
        private java.time.LocalDate lastOrderDate;
    }
}
