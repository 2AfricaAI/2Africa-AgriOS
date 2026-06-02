package ai.toafrica.agrios.service.spi.impl;

import ai.toafrica.agrios.qc.entity.Complaint;
import ai.toafrica.agrios.qc.mapper.ComplaintMapper;
import ai.toafrica.agrios.sales.entity.Customer;
import ai.toafrica.agrios.sales.entity.SalesOrder;
import ai.toafrica.agrios.sales.mapper.CustomerMapper;
import ai.toafrica.agrios.sales.mapper.SalesOrderMapper;
import ai.toafrica.agrios.service.spi.BusinessContextProvider;
import ai.toafrica.agrios.service.vo.ConversationDetailVO.BusinessContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * AgriOS implementation of the CS-Core {@link BusinessContextProvider} SPI.
 *
 * <p>Maps {@code subject_type="customer"} (the value the CS-Core writes into
 * {@code cs_contact_link} when an AgriOS customer is linked) to AgriOS's own
 * {@link Customer} entity, then aggregates the four "vital signs" the agent
 * needs at a glance:</p>
 *
 * <ul>
 *   <li><b>openOrderCount</b> — sales orders not yet completed / cancelled.</li>
 *   <li><b>overdueArInvoiceCount</b> &amp; <b>overdueArAmount</b> — unpaid
 *       orders whose delivery date is more than {@code customer.creditDays}
 *       ago. Defaults to 0-day credit when the customer has no credit term.</li>
 *   <li><b>openComplaintCount</b> — complaints in the open / investigating
 *       phase for this customer.</li>
 *   <li><b>lastOrderDate</b> — the customer's most recent order date — handy
 *       for spotting churn risk.</li>
 * </ul>
 *
 * <p>Other 2Africa products (RetailOS / FactoryOS / TravelOS / AgriCloud /
 * MarketOS) provide their own implementations of this SPI with
 * {@code subjectType()} returning {@code "buyer"} / {@code "client"} /
 * {@code "traveler"} / etc. The CS-Core controller dispatches by
 * {@code subject_type}, so multiple providers can co-exist in a multi-tenant
 * install.</p>
 *
 * <p>Sprint 43D — first wire-up. Counts use simple MyBatis-Plus queries; the
 * volumes per customer are small (single-farm dataset), so we don't need to
 * pre-aggregate yet. When the install grows past ~100k orders per customer
 * we'll switch to a materialised view.</p>
 *
 * <p>Sprint 48a — extracted from the AgriOS-specific
 * {@code BusinessContextService} into this SPI implementation so CS-Core
 * can be lifted into a horizontal module.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgriOsBusinessContextProvider implements BusinessContextProvider {

    private final CustomerMapper customerMapper;
    private final SalesOrderMapper orderMapper;
    private final ComplaintMapper complaintMapper;

    /** Order statuses considered "closed" — anything else counts as open. */
    private static final List<String> CLOSED_ORDER_STATUSES =
            List.of("completed", "cancelled", "closed", "resolved", "void");

    /** Payment statuses we consider not-fully-paid. */
    private static final List<String> UNPAID_PAYMENT_STATUSES =
            List.of("unpaid", "partial", "overdue");

    /** Complaint statuses still demanding attention. */
    private static final List<String> OPEN_COMPLAINT_STATUSES =
            List.of("open", "investigating", "escalated_to_recall");

    /** {@inheritDoc} — AgriOS owns the {@code "customer"} subject type. */
    @Override
    public String subjectType() {
        return "customer";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Loads the {@link Customer} by id then delegates to
     * {@link #forCustomer(Customer)} so existing tests / callers that already
     * have a Customer in hand can re-use the aggregation logic.</p>
     */
    @Override
    public BusinessContext forSubject(Long subjectId) {
        if (subjectId == null) return BusinessContext.builder().build();
        Customer customer = customerMapper.selectById(subjectId);
        return forCustomer(customer);
    }

    /**
     * Compute the business context for a resolved AgriOS customer. Public so
     * callers that already loaded the Customer (e.g. from a join) don't pay
     * the cost of another lookup.
     */
    public BusinessContext forCustomer(Customer customer) {
        if (customer == null) return BusinessContext.builder().build();

        Long customerId = customer.getId();
        int creditDays = customer.getCreditDays() == null ? 0 : customer.getCreditDays();

        // ---- 1. Open orders ----
        Long openOrders = orderMapper.selectCount(
                new LambdaQueryWrapper<SalesOrder>()
                        .eq(SalesOrder::getCustomerId, customerId)
                        .notIn(SalesOrder::getStatus, CLOSED_ORDER_STATUSES)
        );

        // ---- 2. Overdue AR ----
        // Order overdue when delivery_date + customer.creditDays is in the
        // past AND payment_status is anything other than fully paid.
        LocalDate cutoff = LocalDate.now().minusDays(creditDays);
        List<SalesOrder> overdueOrders = orderMapper.selectList(
                new LambdaQueryWrapper<SalesOrder>()
                        .eq(SalesOrder::getCustomerId, customerId)
                        .isNotNull(SalesOrder::getDeliveryDate)
                        .lt(SalesOrder::getDeliveryDate, cutoff)
                        .in(SalesOrder::getPaymentStatus, UNPAID_PAYMENT_STATUSES)
        );
        BigDecimal overdueAmount = overdueOrders.stream()
                .map(o -> safe(o.getTotalAmount()).subtract(safe(o.getPaidAmount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ---- 3. Open complaints ----
        Long openComplaints = complaintMapper.selectCount(
                new LambdaQueryWrapper<Complaint>()
                        .eq(Complaint::getCustomerId, customerId)
                        .in(Complaint::getStatus, OPEN_COMPLAINT_STATUSES)
        );

        // ---- 4. Last order date ----
        SalesOrder lastOrder = orderMapper.selectOne(
                new LambdaQueryWrapper<SalesOrder>()
                        .eq(SalesOrder::getCustomerId, customerId)
                        .isNotNull(SalesOrder::getOrderDate)
                        .orderByDesc(SalesOrder::getOrderDate)
                        .last("LIMIT 1")
        );

        return BusinessContext.builder()
                .openOrderCount(openOrders == null ? 0 : openOrders.intValue())
                .overdueArInvoiceCount(overdueOrders.size())
                .overdueArAmount(overdueAmount.signum() == 0 ? null : overdueAmount)
                .openComplaintCount(openComplaints == null ? 0 : openComplaints.intValue())
                .lastOrderDate(lastOrder == null ? null : lastOrder.getOrderDate())
                .build();
    }

    private static BigDecimal safe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
