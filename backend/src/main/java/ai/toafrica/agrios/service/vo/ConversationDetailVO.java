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

    /**
     * Sprint 47 — WhatsApp zero-cost policy state. Always populated.
     * {@code managed=false} means this isn't a WhatsApp inbox and the UI
     * should hide the countdown chip + service-window banner entirely.
     */
    private WhatsAppPolicy whatsAppPolicy;

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

    /**
     * Sprint 47 — projection of {@code WhatsAppPolicyService.Decision} for
     * the frontend. UI uses {@code serviceWindowExpiresAt} to render a
     * countdown chip, and {@code withinServiceWindow} to decide whether
     * the "公开回复" tab is enabled.
     */
    @Data
    @Builder
    public static class WhatsAppPolicy {
        /** True when this conversation's inbox is governed by the policy. */
        private Boolean managed;
        /** Latest customer inbound timestamp (epoch sec); null if never. */
        private Long lastInboundAt;
        /** When the free reply window closes (epoch sec); null if N/A. */
        private Long serviceWindowExpiresAt;
        /** True when a public agent reply right now would be free. */
        private Boolean withinServiceWindow;
    }
}
