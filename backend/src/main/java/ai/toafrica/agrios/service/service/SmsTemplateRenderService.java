package ai.toafrica.agrios.service.service;

import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.finance.entity.SmsTemplate;
import ai.toafrica.agrios.finance.mapper.SmsTemplateMapper;
import ai.toafrica.agrios.sales.entity.Customer;
import ai.toafrica.agrios.sales.entity.SalesOrder;
import ai.toafrica.agrios.sales.mapper.CustomerMapper;
import ai.toafrica.agrios.sales.mapper.SalesOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders an SMS / WhatsApp template against the resolved AgriOS customer
 * context. Supported placeholders (Sprint 18 originally):
 *
 * <pre>
 *   {customerName}   {orderCode}     {amount}
 *   {currency}       {dueDate}       {daysOverdue}
 *   {farmName}       {today}
 * </pre>
 *
 * <p>The Sprint 18 sms_template table is reused as-is. Sprint 45 (F) adds
 * the rendering on top so CSR agents can populate the conversation
 * composer with a one-click template fill — same content the AR collection
 * cron job uses, so the wording stays consistent across channels.</p>
 *
 * <p>When the customer has no overdue order, order-related placeholders
 * fall back to neutral defaults ({@code "your invoice"} for orderCode,
 * empty string for amount/dueDate/daysOverdue) so the text still reads
 * naturally.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsTemplateRenderService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMM yyyy");

    /** Order payment statuses we treat as not-fully-paid. */
    private static final List<String> UNPAID = List.of("unpaid", "partial", "overdue");

    private final SmsTemplateMapper templateMapper;
    private final SalesOrderMapper orderMapper;
    private final CustomerMapper customerMapper;

    /**
     * List active templates for the dropdown. The CSR UI shows the name +
     * a short preview of the body so the agent picks the right one before
     * the placeholders get resolved.
     */
    public List<SmsTemplate> listTemplates() {
        return templateMapper.selectList(
                new LambdaQueryWrapper<SmsTemplate>()
                        .eq(SmsTemplate::getEnabled, 1)
                        .orderByAsc(SmsTemplate::getCode)
        );
    }

    /**
     * Render the template by code against the AgriOS customer (and optional
     * SalesOrder) the placeholders need.
     *
     * @param templateCode e.g. {@code AR_OVERDUE}
     * @param customerId   AgriOS Customer.id (required for any meaningful
     *                     template — without it we just substitute defaults)
     */
    public String render(String templateCode, Long customerId) {
        SmsTemplate tpl = templateMapper.selectOne(
                new LambdaQueryWrapper<SmsTemplate>().eq(SmsTemplate::getCode, templateCode)
        );
        if (tpl == null) {
            throw new BusinessException("Unknown SMS template: " + templateCode);
        }
        Customer customer = customerId == null ? null : customerMapper.selectById(customerId);
        Map<String, String> ctx = buildContext(customer);
        return substitute(tpl.getContent(), ctx);
    }

    /**
     * Build the placeholder map. Pulls the customer's most recent unpaid /
     * partial order so order-related placeholders are about a real invoice
     * rather than "the order we sent you" generic text.
     */
    private Map<String, String> buildContext(Customer customer) {
        Map<String, String> ctx = new HashMap<>();
        // Defaults — overwritten below if we have real data.
        ctx.put("customerName", customer == null ? "there" : safe(customer.getName(), "there"));
        ctx.put("farmName", "Albert's Farm"); // TODO Sprint 46: lookup from sys_config
        ctx.put("today", LocalDate.now().format(DATE_FMT));
        ctx.put("currency", "KES");
        ctx.put("orderCode", "your invoice");
        ctx.put("amount", "");
        ctx.put("dueDate", "");
        ctx.put("daysOverdue", "0");

        if (customer == null) return ctx;

        // Find the most recent unpaid order (for AR-themed templates).
        SalesOrder order = orderMapper.selectOne(
                new LambdaQueryWrapper<SalesOrder>()
                        .eq(SalesOrder::getCustomerId, customer.getId())
                        .in(SalesOrder::getPaymentStatus, UNPAID)
                        .orderByDesc(SalesOrder::getOrderDate)
                        .last("LIMIT 1")
        );
        if (order != null) {
            ctx.put("orderCode", safe(order.getCode(), "your invoice"));
            BigDecimal outstanding = safe(order.getTotalAmount()).subtract(safe(order.getPaidAmount()));
            ctx.put("amount", formatAmount(outstanding));
            // Currency from the order if present, else the default KES.
            if (notBlank(order.getCurrency())) ctx.put("currency", order.getCurrency());

            // Due date = delivery_date + customer.creditDays (best effort).
            LocalDate due = computeDueDate(order, customer);
            if (due != null) {
                ctx.put("dueDate", due.format(DATE_FMT));
                long overdueDays = ChronoUnit.DAYS.between(due, LocalDate.now());
                ctx.put("daysOverdue", String.valueOf(Math.max(0L, overdueDays)));
            }
        }
        return ctx;
    }

    /** Replace every {key} with its value; leaves unknown {placeholders} untouched. */
    private String substitute(String content, Map<String, String> ctx) {
        if (content == null || content.isEmpty()) return "";
        String out = content;
        for (Map.Entry<String, String> e : ctx.entrySet()) {
            out = out.replace("{" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
        }
        return out;
    }

    private LocalDate computeDueDate(SalesOrder order, Customer customer) {
        LocalDate delivery = order.getDeliveryDate();
        if (delivery == null) return null;
        int creditDays = customer.getCreditDays() == null ? 0 : customer.getCreditDays();
        return delivery.plusDays(creditDays);
    }

    private static String formatAmount(BigDecimal v) {
        if (v == null || v.signum() == 0) return "";
        return v.toPlainString().replaceAll("\\.0+$", "");
    }

    private static BigDecimal safe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static String safe(String s, String fallback) {
        return (s == null || s.isBlank()) ? fallback : s;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    /** No-template-found case for the UI: returns empty list instead of throwing. */
    public List<SmsTemplate> safeListTemplates() {
        try {
            return listTemplates();
        } catch (Exception e) {
            log.warn("[SmsTemplate] list failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
