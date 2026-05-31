package ai.toafrica.agrios.service.service;

import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.sales.entity.Customer;
import ai.toafrica.agrios.sales.mapper.CustomerMapper;
import ai.toafrica.agrios.service.client.ChatwootClient;
import ai.toafrica.agrios.service.client.dto.ChatwootContact;
import ai.toafrica.agrios.service.client.dto.ChatwootContactRequest;
import ai.toafrica.agrios.service.config.ChatwootProperties;
import ai.toafrica.agrios.service.entity.ServiceContactLink;
import ai.toafrica.agrios.service.mapper.ServiceContactLinkMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Sync an AgriOS {@link Customer} to a Chatwoot Contact.
 *
 * <p>Direction is one-way for now: AgriOS is the source of truth for customer
 * master data, Chatwoot is the source of truth for conversation history. We
 * push customer updates into Chatwoot; the reverse direction (Chatwoot creating
 * a contact that isn't yet in AgriOS) is handled by Sprint 40d's webhook.</p>
 *
 * <p>Idempotency: the link table's {@code uk_chatwoot_contact} unique key and
 * the search-before-create flow together make {@link #syncCustomer(Long)} safe
 * to call repeatedly — re-runs become no-op PATCHes when nothing has drifted.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContactSyncService {

    private final ChatwootProperties props;
    private final ChatwootClient chatwoot;
    private final CustomerMapper customerMapper;
    private final ServiceContactLinkMapper linkMapper;
    private final ServiceEventLogger eventLogger;

    /** AgriOS entity_type written into the link table. */
    private static final String ENTITY_TYPE_CUSTOMER = "customer";

    /**
     * Push a single customer to Chatwoot. Returns the resolved link row.
     *
     * @throws BusinessException if Chatwoot is unreachable or returns an error
     * @throws IllegalStateException if Chatwoot integration is disabled (no API token)
     */
    @Transactional
    public ServiceContactLink syncCustomer(Long customerId) {
        if (!props.isEnabled()) {
            throw new IllegalStateException("Chatwoot integration disabled — set agrios.chatwoot.api-token");
        }

        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new BusinessException("Customer not found: " + customerId);
        }

        ServiceContactLink link = findExistingLink(customer.getId());

        try {
            ChatwootContact remote = (link != null)
                    ? updateInChatwoot(link, customer)
                    : createOrAdoptInChatwoot(customer);

            link = upsertLink(link, customer, remote, "ok", null);
            eventLogger.ok("sync.customer_push", "outbound", ENTITY_TYPE_CUSTOMER, customer.getId(),
                    Map.of("chatwootContactId", remote.getId(), "code", customer.getCode()));
            log.info("[ContactSync] customer={} -> chatwoot_contact={} ok", customer.getCode(), remote.getId());
            return link;

        } catch (Exception e) {
            // Persist the failure so the dashboard can show a broken-link badge.
            link = upsertLink(link, customer, null, "error", trim(e.getMessage(), 500));
            eventLogger.failed("sync.customer_push", "outbound", ENTITY_TYPE_CUSTOMER, customer.getId(),
                    Map.of("code", customer.getCode()), e.getMessage());
            log.error("[ContactSync] customer={} failed: {}", customer.getCode(), e.getMessage());
            throw new BusinessException("Chatwoot sync failed for customer " + customer.getCode() + ": " + e.getMessage());
        }
    }

    /**
     * Bulk push every active customer that isn't already linked-and-ok. Intended
     * for one-shot operator action ("first time setup") rather than a daily job.
     */
    public BulkResult syncAllActive() {
        if (!props.isEnabled()) {
            throw new IllegalStateException("Chatwoot integration disabled — set agrios.chatwoot.api-token");
        }

        LambdaQueryWrapper<Customer> q = new LambdaQueryWrapper<Customer>()
                .eq(Customer::getStatus, "active")
                .orderByAsc(Customer::getId);
        List<Customer> all = customerMapper.selectList(q);

        int ok = 0, failed = 0;
        for (Customer c : all) {
            try {
                syncCustomer(c.getId());
                ok++;
            } catch (Exception e) {
                failed++;
                // syncCustomer already logged and persisted the failure — keep going.
            }
        }
        return new BulkResult(all.size(), ok, failed);
    }

    /** Result of a bulk sync, returned to the controller. */
    public record BulkResult(int total, int ok, int failed) {}

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private ServiceContactLink findExistingLink(Long customerId) {
        LambdaQueryWrapper<ServiceContactLink> q = new LambdaQueryWrapper<ServiceContactLink>()
                .eq(ServiceContactLink::getAgriosEntityType, ENTITY_TYPE_CUSTOMER)
                .eq(ServiceContactLink::getAgriosEntityId, customerId);
        return linkMapper.selectOne(q);
    }

    /**
     * Push existing-link case: PUT to Chatwoot. If the remote row was deleted
     * out from under us, fall back to the create-or-adopt path.
     */
    private ChatwootContact updateInChatwoot(ServiceContactLink link, Customer customer) {
        try {
            return chatwoot.updateContact(link.getChatwootContactId(), buildRequest(customer));
        } catch (Exception e) {
            log.warn("[ContactSync] update failed for chatwoot_contact_id={}, will try to re-create: {}",
                    link.getChatwootContactId(), e.getMessage());
            return createOrAdoptInChatwoot(customer);
        }
    }

    /**
     * No-link case: first look in Chatwoot for an existing contact with our
     * identifier (handles the "operator wiped service_contact_link table" case),
     * otherwise create a fresh one.
     */
    private ChatwootContact createOrAdoptInChatwoot(Customer customer) {
        Optional<ChatwootContact> existing = chatwoot.findByIdentifier(customer.getCode());
        if (existing.isPresent()) {
            log.info("[ContactSync] adopting existing chatwoot contact id={} for customer={}",
                    existing.get().getId(), customer.getCode());
            return chatwoot.updateContact(existing.get().getId(), buildRequest(customer));
        }
        return chatwoot.createContact(buildRequest(customer));
    }

    /**
     * Map an AgriOS Customer to the Chatwoot contact payload.
     *
     * <p>The {@code identifier} field is our long-term cross-system handle —
     * we use Customer.code (e.g. CUS-00042) so a human looking in Chatwoot
     * can copy/paste it straight back into AgriOS.</p>
     */
    private ChatwootContactRequest buildRequest(Customer customer) {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("agrios_customer_id", customer.getId());
        attrs.put("agrios_customer_code", customer.getCode());
        if (customer.getType() != null) attrs.put("agrios_customer_type", customer.getType());
        if (customer.getCreditLevel() != null) attrs.put("agrios_credit_level", customer.getCreditLevel());
        if (customer.getPaymentTerms() != null) attrs.put("agrios_payment_terms", customer.getPaymentTerms());

        return ChatwootContactRequest.builder()
                .name(customer.getName())
                .phoneNumber(normalizePhone(customer.getContactPhone()))
                .identifier(customer.getCode())
                .customAttributes(attrs)
                .build();
    }

    /**
     * Bare-minimum phone normalization. Kenyan numbers entered as
     * {@code 07XXXXXXXX} get the {@code +254} prefix; anything else passes
     * through. We will revisit when we have more international customers.
     */
    private String normalizePhone(String raw) {
        if (raw == null) return null;
        String s = raw.replaceAll("[\\s\\-()]", "");
        if (s.isEmpty()) return null;
        if (s.startsWith("+")) return s;
        if (s.startsWith("00")) return "+" + s.substring(2);
        if (s.startsWith("254")) return "+" + s;
        if (s.startsWith("0") && s.length() == 10) return "+254" + s.substring(1);
        return s;
    }

    private ServiceContactLink upsertLink(ServiceContactLink existing, Customer customer,
                                          ChatwootContact remote, String status, String error) {
        if (existing == null) {
            ServiceContactLink row = new ServiceContactLink();
            row.setAgriosEntityType(ENTITY_TYPE_CUSTOMER);
            row.setAgriosEntityId(customer.getId());
            row.setChatwootContactId(remote != null ? remote.getId() : 0L);
            row.setChatwootAccountId(props.getAccountId());
            row.setLastSyncedAt(remote != null ? LocalDateTime.now() : null);
            row.setSyncStatus(status);
            row.setSyncError(error);
            linkMapper.insert(row);
            return row;
        }
        if (remote != null) {
            existing.setChatwootContactId(remote.getId());
            existing.setLastSyncedAt(LocalDateTime.now());
        }
        existing.setSyncStatus(status);
        existing.setSyncError(error);
        linkMapper.updateById(existing);
        return existing;
    }

    private static String trim(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
