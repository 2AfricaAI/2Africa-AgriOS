package ai.toafrica.agrios.service.controller;

import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.sales.entity.Customer;
import ai.toafrica.agrios.sales.mapper.CustomerMapper;
import ai.toafrica.agrios.service.client.ChatwootClient;
import ai.toafrica.agrios.service.client.dto.ChatwootAgent;
import ai.toafrica.agrios.service.client.dto.ChatwootContact;
import ai.toafrica.agrios.service.client.dto.ChatwootConversation;
import ai.toafrica.agrios.service.client.dto.ChatwootInbox;
import ai.toafrica.agrios.service.entity.CsContactLink;
import ai.toafrica.agrios.service.mapper.CsContactLinkMapper;
import ai.toafrica.agrios.finance.entity.SmsTemplate;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.service.spi.BusinessContextProvider;
import ai.toafrica.agrios.service.service.SmsTemplateRenderService;
import ai.toafrica.agrios.service.service.WhatsAppPolicyService;
import ai.toafrica.agrios.service.vo.ConversationDetailVO;
import ai.toafrica.agrios.service.vo.ConversationListItemVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Conversations / inboxes / agents — the read+write endpoints driving the
 * AgriOS-native Customer Service UI. All calls pass through
 * {@link ChatwootClient}; the controller's job is to enrich Chatwoot data
 * with AgriOS business context (customer record, orders, AR, complaints)
 * so the frontend can render one cohesive view.
 *
 * <p>Auth: JWT required (same as the rest of /v1/service/*). Fine-grained
 * RBAC will be added in Sprint 42 once the seed file ships
 * {@code service:conversation:read|write} permissions.</p>
 */
@Slf4j
@Tag(name = "91 · Service - Conversations", description = "AgriOS-native conversation workspace")
@RestController
@RequestMapping({"/v1/cs", "/v1/service"})
@RequiredArgsConstructor
public class ConversationController {

    private final ChatwootClient chatwoot;
    private final CsContactLinkMapper linkMapper;
    private final CustomerMapper customerMapper;
    /**
     * CS-Core SPI (Sprint 48a). Pluggable per consuming product — AgriOS
     * ships {@code AgriOsBusinessContextProvider}. Other products plug their
     * own {@code BusinessContextProvider} bean and the bean wiring picks the
     * right one at startup.
     */
    private final BusinessContextProvider businessContext;
    private final SmsTemplateRenderService templateRender;
    private final WhatsAppPolicyService whatsAppPolicy;

    // -----------------------------------------------------------------------
    // Conversations
    // -----------------------------------------------------------------------

    @Operation(summary = "List conversations with optional filters")
    @GetMapping("/conversations")
    public R<List<ConversationListItemVO>> list(
            @RequestParam(required = false, defaultValue = "open") String status,
            @RequestParam(required = false) Long inboxId,
            @RequestParam(required = false) String assigneeType,
            @RequestParam(required = false, defaultValue = "1") Integer page
    ) {
        Map<String, String> q = new HashMap<>();
        q.put("status", status);
        if (inboxId != null) q.put("inbox_id", inboxId.toString());
        if (assigneeType != null && !assigneeType.isBlank()) q.put("assignee_type", assigneeType);
        if (page != null) q.put("page", page.toString());

        List<ChatwootConversation> raw = chatwoot.listConversations(q);
        List<ConversationListItemVO> rows = raw.stream()
                .map(ConversationListItemVO::from)
                .toList();

        // Bulk-resolve AgriOS customers for the contact ids we just collected.
        // Single round-trip to the link table + customers table.
        Map<Long, CsContactLink> linkByContact = bulkLookupLinks(
                raw.stream()
                   .map(c -> c.resolvedContact() != null ? c.resolvedContact().getId() : null)
                   .filter(java.util.Objects::nonNull)
                   .collect(Collectors.toSet())
        );

        for (int i = 0; i < rows.size(); i++) {
            ChatwootConversation c = raw.get(i);
            Long contactId = c.resolvedContact() != null ? c.resolvedContact().getId() : null;
            if (contactId == null) continue;
            CsContactLink link = linkByContact.get(contactId);
            if (link == null) continue;
            Customer cust = customerMapper.selectById(link.getSubjectId());
            if (cust == null) continue;
            rows.get(i).setAgriosCustomerId(cust.getId());
            rows.get(i).setAgriosCustomerCode(cust.getCode());
        }
        return R.ok(rows);
    }

    @Operation(summary = "Get conversation detail + messages + AgriOS business context")
    @GetMapping("/conversations/{id}")
    public R<ConversationDetailVO> detail(@PathVariable Long id) {
        ChatwootConversation c = chatwoot.getConversation(id);

        List<ChatwootClient.ChatMessage> msgs = chatwoot.listMessages(id);
        // Chatwoot returns newest-first; flip for chronological UI rendering.
        List<ChatwootClient.ChatMessage> ordered = new java.util.ArrayList<>(msgs);
        java.util.Collections.reverse(ordered);

        List<ConversationDetailVO.MessageVO> mvs = ordered.stream()
                .map(m -> ConversationDetailVO.MessageVO.builder()
                        .id(m.id)
                        .content(m.content)
                        .messageType(m.fromCustomer ? 0 : 1)
                        .privateNote(m.privateNote)
                        .createdAt(m.createdAt)
                        .build())
                .toList();

        // Sprint 47: evaluate WhatsApp service-window policy once and ship
        // the result alongside the conversation so the UI can render the
        // countdown chip and decide whether public reply is enabled.
        WhatsAppPolicyService.Decision waDecision = whatsAppPolicy.evaluate(c);

        var builder = ConversationDetailVO.builder()
                .id(c.getId())
                .displayId(c.getDisplayId() != null ? c.getDisplayId() : c.getId())
                .status(c.getStatus())
                .channel(c.resolvedChannel())
                .inboxId(c.getInboxId())
                .assigneeId(c.getAssigneeId())
                .whatsAppPolicy(ConversationDetailVO.WhatsAppPolicy.builder()
                        .managed(waDecision.managed())
                        .lastInboundAt(waDecision.lastInboundAtEpochSec())
                        .serviceWindowExpiresAt(waDecision.serviceWindowExpiresAtEpochSec())
                        .withinServiceWindow(waDecision.withinServiceWindow())
                        .build())
                .messages(mvs);

        // Resolve AgriOS Customer if linked.
        Long contactId = c.resolvedContact() != null ? c.resolvedContact().getId() : null;
        if (contactId != null) {
            CsContactLink link = linkMapper.selectOne(
                    new LambdaQueryWrapper<CsContactLink>()
                            .eq(CsContactLink::getChatwootContactId, contactId)
                            .last("LIMIT 1"));
            if (link != null) {
                Customer cust = customerMapper.selectById(link.getSubjectId());
                if (cust != null) {
                    builder.customer(ConversationDetailVO.AgriosCustomerSummary.builder()
                            .id(cust.getId())
                            .code(cust.getCode())
                            .name(cust.getName())
                            .type(cust.getType())
                            .contactName(cust.getContactName())
                            .contactPhone(cust.getContactPhone())
                            .creditLevel(cust.getCreditLevel())
                            .paymentTerms(cust.getPaymentTerms())
                            .creditDays(cust.getCreditDays())
                            .build());

                    // Sprint 43D: wire real counts from sales/finance/qc.
                    // Sprint 48a: dispatch via the CS-Core SPI so the
                    // controller no longer hard-codes AgriOS aggregation.
                    builder.businessContext(businessContext.forSubject(link.getSubjectId()));
                }
            }
        }
        return R.ok(builder.build());
    }

    @Operation(summary = "Reply to a conversation — public outgoing OR private agent note")
    @PostMapping("/conversations/{id}/messages")
    public R<Void> reply(@PathVariable Long id, @RequestBody ReplyBody body) {
        if (body == null || body.getContent() == null || body.getContent().isBlank()) {
            return R.ok();
        }
        boolean isPrivate = Boolean.TRUE.equals(body.getPrivateNote());

        // Sprint 47 — WhatsApp zero-cost policy enforcement.
        // Private notes are never sent to the customer, so they cost
        // nothing and are always allowed. Public replies on WhatsApp
        // inboxes are gated by the 24h service window.
        if (!isPrivate) {
            ChatwootConversation conv = chatwoot.getConversation(id);
            WhatsAppPolicyService.Decision d = whatsAppPolicy.evaluate(conv);
            if (d.managed() && !d.withinServiceWindow()) {
                log.info("[WhatsApp policy] BLOCK public reply on conv#{} — "
                        + "service window expired (lastInboundAt={}, expiresAt={})",
                        id, d.lastInboundAtEpochSec(), d.serviceWindowExpiresAtEpochSec());
                throw new BusinessException(40901,
                        "WhatsApp 24h service window expired; public reply "
                        + "blocked by zero-cost policy. Use a private note "
                        + "for internal follow-up, or contact the customer "
                        + "via SMS / Email.");
            }
        }

        if (isPrivate) {
            chatwoot.sendPrivateNote(id, body.getContent());
        } else {
            chatwoot.sendOutboundMessage(id, body.getContent());
        }
        return R.ok();
    }

    @Operation(summary = "Toggle conversation status (open / resolved / pending / snoozed)")
    @PostMapping("/conversations/{id}/status")
    public R<Void> changeStatus(@PathVariable Long id, @RequestBody StatusBody body) {
        chatwoot.toggleConversationStatus(id, body.getStatus());
        return R.ok();
    }

    @Operation(summary = "Assign a conversation to an agent (null = unassign)")
    @PostMapping("/conversations/{id}/assignee")
    public R<Void> assign(@PathVariable Long id, @RequestBody AssignBody body) {
        chatwoot.assignAgent(id, body.getAssigneeId());
        return R.ok();
    }

    /**
     * Sprint 49.5 -- destructive single-conversation delete. Restricted to
     * SUPER_ADMIN via the {@code cs:conversation:delete} permission so the
     * roster of regular agents cannot accidentally wipe a customer history.
     *
     * <p>Implementation simply forwards the call to Chatwoot, which cascades
     * delete to messages + attachments + conversation_participants in its
     * own DB. CS-Core's {@code cs_contact_link} row is untouched (other
     * conversations from the same contact may still exist).</p>
     */
    @Operation(summary = "Delete a single conversation (SUPER_ADMIN only)")
    @DeleteMapping("/conversations/{id}")
    @org.springframework.security.access.prepost.PreAuthorize(
            "hasAuthority('cs:conversation:delete')")
    public R<Void> delete(@PathVariable Long id) {
        log.warn("[cs] DELETE conversation#{} by SUPER_ADMIN", id);
        chatwoot.deleteConversation(id);
        return R.ok();
    }

    // -----------------------------------------------------------------------
    // Inboxes & Agents — read-only enumerations for UI dropdowns
    // -----------------------------------------------------------------------

    @Operation(summary = "List Chatwoot inboxes")
    @GetMapping("/inboxes")
    public R<List<ChatwootInbox>> inboxes() {
        return R.ok(chatwoot.listInboxes());
    }

    @Operation(summary = "List Chatwoot agents (CSR users)")
    @GetMapping("/agents")
    public R<List<ChatwootAgent>> agents() {
        return R.ok(chatwoot.listAgents());
    }

    // -----------------------------------------------------------------------
    // Sprint 45: SMS / WhatsApp templates — list + render against a conv
    // -----------------------------------------------------------------------

    @Operation(summary = "List available SMS / WhatsApp templates")
    @GetMapping("/sms-templates")
    public R<List<SmsTemplate>> listSmsTemplates() {
        return R.ok(templateRender.safeListTemplates());
    }

    @Operation(summary = "Render a template against the given conversation's resolved customer")
    @PostMapping("/sms-templates/render")
    public R<Map<String, Object>> renderSmsTemplate(@RequestBody RenderTemplateBody body) {
        if (body == null || body.getCode() == null || body.getCode().isBlank()) {
            throw new ai.toafrica.agrios.common.exception.BusinessException("code is required");
        }

        Long customerId = body.getCustomerId();
        if (customerId == null && body.getConversationId() != null) {
            // Resolve customer via conversation → service_contact_link
            ChatwootConversation c = chatwoot.getConversation(body.getConversationId());
            Long contactId = c.resolvedContact() != null ? c.resolvedContact().getId() : null;
            if (contactId != null) {
                CsContactLink link = linkMapper.selectOne(
                        new LambdaQueryWrapper<CsContactLink>()
                                .eq(CsContactLink::getChatwootContactId, contactId)
                                .last("LIMIT 1"));
                if (link != null) customerId = link.getSubjectId();
            }
        }

        String rendered = templateRender.render(body.getCode(), customerId);
        return R.ok(Map.of(
                "code", body.getCode(),
                "customerId", customerId == null ? "" : customerId,
                "rendered", rendered
        ));
    }

    @lombok.Data
    public static class RenderTemplateBody {
        private String code;
        private Long conversationId;
        private Long customerId;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Map<Long, CsContactLink> bulkLookupLinks(java.util.Set<Long> chatwootContactIds) {
        if (chatwootContactIds == null || chatwootContactIds.isEmpty()) {
            return Map.of();
        }
        List<CsContactLink> links = linkMapper.selectList(
                new LambdaQueryWrapper<CsContactLink>()
                        .in(CsContactLink::getChatwootContactId, chatwootContactIds));
        return links.stream()
                .collect(Collectors.toMap(CsContactLink::getChatwootContactId,
                                          l -> l, (a, b) -> a));
    }

    // -----------------------------------------------------------------------
    // Request bodies
    // -----------------------------------------------------------------------

    @lombok.Data
    public static class ReplyBody {
        /** Message text the agent typed. */
        private String content;
        /**
         * When true, persisted as a Chatwoot private note (visible only to the
         * agent team) instead of an outbound customer-facing message. Used by
         * the AgriOS UI's "内部私信" mode and by the Sprint 47 WhatsApp
         * service-window fallback path.
         */
        private Boolean privateNote;
    }

    @lombok.Data
    public static class StatusBody {
        /** "open" / "resolved" / "pending" / "snoozed". */
        private String status;
    }

    @lombok.Data
    public static class AssignBody {
        /** Chatwoot agent id, or {@code null} to unassign. */
        private Long assigneeId;
    }
}
