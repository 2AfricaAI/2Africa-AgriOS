package ai.toafrica.agrios.service.service;

import ai.toafrica.agrios.qc.entity.Complaint;
import ai.toafrica.agrios.qc.mapper.ComplaintMapper;
import ai.toafrica.agrios.sales.entity.Customer;
import ai.toafrica.agrios.sales.entity.SalesOrder;
import ai.toafrica.agrios.sales.mapper.SalesOrderMapper;
import ai.toafrica.agrios.service.vo.ConversationDetailVO.BusinessContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Aggregates the customer "vital signs" the CSR agent needs at a glance the
 * moment they open a conversation:
 *
 * <ul>
 *   <li><b>openOrderCount</b> — sales orders not yet completed / cancelled.</li>
 *   <li><b>overdueArInvoiceCount</b> &amp; <b>overdueArAmount</b> — unpaid
 *       orders whose delivery date is more than {@code customer.creditDays}
 *       ago. Defaults to 0-day credit when the customer has no credit term.</li>
 *   <li><b>openComplaintCount</b> — complaints in the open / investigating
 *       phase for this customer.</li>
 *   <li><b>lastOrderDate</b> — the date of the customer's most recent order,
 *       handy for spotting churn risk.</li>
 * </ul>
 *
 * <p>Sprint 43D — first wire-up. Counts use simple MyBatis-Plus queries; the
 * volumes per customer are small (single-farm dataset), so we don't need to
 * pre-aggregate yet. When the install grows past ~100k orders per customer
 * we'll switch to a materialised view.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessContextService {

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

    /**
     * Compute the business context for a linked AgriOS customer. The
     * conversation controller is expected to pass in the resolved
     * {@link Customer} (after looking it up via service_contact_link). If the
     * conversation is unlinked we return an empty context.
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
        // We treat an order as overdue when delivery_date + customer.creditDays
        // is in the past AND payment_status indicates anything other than fully paid.
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
