package ai.toafrica.agrios.service.controller;

import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.sales.entity.Customer;
import ai.toafrica.agrios.sales.mapper.CustomerMapper;
import ai.toafrica.agrios.service.client.ChatwootClient;
import ai.toafrica.agrios.service.client.dto.ChatwootAgent;
import ai.toafrica.agrios.service.client.dto.ChatwootContact;
import ai.toafrica.agrios.service.client.dto.ChatwootConversation;
import ai.toafrica.agrios.service.client.dto.ChatwootInbox;
import ai.toafrica.agrios.service.entity.ServiceContactLink;
import ai.toafrica.agrios.service.mapper.ServiceContactLinkMapper;
import ai.toafrica.agrios.service.service.BusinessContextService;
import ai.toafrica.agrios.service.vo.ConversationDetailVO;
import ai.toafrica.agrios.service.vo.ConversationListItemVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequestMapping("/v1/service")
@RequiredArgsConstructor
public class ConversationController {

    private final ChatwootClient chatwoot;
    private final ServiceContactLinkMapper linkMapper;
    private final CustomerMapper customerMapper;
    private final BusinessContextService businessContext;

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
        Map<Long, ServiceContactLink> linkByContact = bulkLookupLinks(
                raw.stream()
                   .map(c -> c.getContact() != null ? c.getContact().getId()
                           : (c.getMeta() != null ? c.getMeta().getId() : null))
                   .filter(java.util.Objects::nonNull)
                   .collect(Collectors.toSet())
        );

        for (int i = 0; i < rows.size(); i++) {
            ChatwootConversation c = raw.get(i);
            Long contactId = c.getContact() != null ? c.getContact().getId()
                    : (c.getMeta() != null ? c.getMeta().getId() : null);
            if (contactId == null) continue;
            ServiceContactLink link = linkByContact.get(contactId);
            if (link == null) continue;
            Customer cust = customerMapper.selectById(link.getAgriosEntityId());
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

        var builder = ConversationDetailVO.builder()
                .id(c.getId())
                .displayId(c.getDisplayId())
                .status(c.getStatus())
                .channel(c.getChannel())
                .inboxId(c.getInboxId())
                .assigneeId(c.getAssigneeId())
                .messages(mvs);

        // Resolve AgriOS Customer if linked.
        Long contactId = c.getContact() != null ? c.getContact().getId()
                : (c.getMeta() != null ? c.getMeta().getId() : null);
        if (contactId != null) {
            ServiceContactLink link = linkMapper.selectOne(
                    new LambdaQueryWrapper<ServiceContactLink>()
                            .eq(ServiceContactLink::getChatwootContactId, contactId)
                            .last("LIMIT 1"));
            if (link != null) {
                Customer cust = customerMapper.selectById(link.getAgriosEntityId());
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
                    builder.businessContext(businessContext.forCustomer(cust));
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
        if (Boolean.TRUE.equals(body.getPrivateNote())) {
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
    // Helpers
    // -----------------------------------------------------------------------

    private Map<Long, ServiceContactLink> bulkLookupLinks(java.util.Set<Long> chatwootContactIds) {
        if (chatwootContactIds == null || chatwootContactIds.isEmpty()) {
            return Map.of();
        }
        List<ServiceContactLink> links = linkMapper.selectList(
                new LambdaQueryWrapper<ServiceContactLink>()
                        .in(ServiceContactLink::getChatwootContactId, chatwootContactIds));
        return links.stream()
                .collect(Collectors.toMap(ServiceContactLink::getChatwootContactId,
                                          l -> l, (a, b) -> a));
    }

    // -----------------------------------------------------------------------
    // Request bodies
    // -----------------------------------------------------------------------

    @lombok.Data
    public static class ReplyBody {
        private String content;
        /** When true, message is sent as a private note (visible to agents only). */
        private Boolean privateNote = false;
    }

    @lombok.Data
    public static class StatusBody {
        private String status;
    }

    @lombok.Data
    public static class AssignBody {
        private Long assigneeId;
    }
}
