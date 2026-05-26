package ai.toafrica.agrios.ops.service.rule;

import ai.toafrica.agrios.ops.entity.ActionItem;
import ai.toafrica.agrios.sales.entity.Customer;
import ai.toafrica.agrios.sales.entity.SalesOrder;
import ai.toafrica.agrios.sales.mapper.CustomerMapper;
import ai.toafrica.agrios.sales.mapper.SalesOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * R-AR-01 应收逾期规则 - Sprint 16.3 (升级为真实实现)
 *
 *   扫描所有 sales_order:
 *     status NOT IN (cancelled, returned)
 *     payment_status <> 'paid'
 *     due_date < today
 *
 *   每个命中的订单生成一条 ActionItem (ref_type=order, ref_id=order.id):
 *     1-7   天逾期 → severity=low,    category=followup
 *     8-30  天逾期 → severity=medium, category=today
 *     31+   天逾期 → severity=high,   category=today
 *
 *   refresh() 会自动 upsert 并把已收清的订单转 auto_resolved.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueArRule implements ActionRule {

    private final SalesOrderMapper orderMapper;
    private final CustomerMapper customerMapper;

    @Override public String ruleCode()  { return "R-AR-01"; }
    @Override public String category()  { return "today"; }       // 默认 (实际按天数动态)
    @Override public String severity()  { return "high"; }        // 默认 (实际按天数动态)
    @Override public String ownerRole() { return "finance"; }

    @Override
    public List<ActionItem> evaluate() {
        LocalDate today = LocalDate.now();

        // 拉取所有逾期未付订单
        List<SalesOrder> overdue = orderMapper.selectList(
                new LambdaQueryWrapper<SalesOrder>()
                        .notIn(SalesOrder::getStatus, "cancelled", "returned")
                        .ne(SalesOrder::getPaymentStatus, "paid")
                        .isNotNull(SalesOrder::getDueDate)
                        .lt(SalesOrder::getDueDate, today)
                        .orderByAsc(SalesOrder::getDueDate));

        if (overdue.isEmpty()) {
            return List.of();
        }

        // 批量取客户名 (避免循环里 N 次单查)
        Map<Long, String> customerNameById = new HashMap<>();
        for (SalesOrder o : overdue) {
            customerNameById.computeIfAbsent(o.getCustomerId(), id -> {
                Customer c = customerMapper.selectById(id);
                return c != null ? c.getName() : "Unknown";
            });
        }

        List<ActionItem> out = new ArrayList<>();
        for (SalesOrder o : overdue) {
            long days = ChronoUnit.DAYS.between(o.getDueDate(), today);
            if (days <= 0) continue;

            String sev;
            String cat;
            if (days >= 31)      { sev = "high";   cat = "today"; }
            else if (days >= 8)  { sev = "medium"; cat = "today"; }
            else                 { sev = "low";    cat = "followup"; }

            BigDecimal total = o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal paid  = o.getPaidAmount()  != null ? o.getPaidAmount()  : BigDecimal.ZERO;
            BigDecimal outstanding = total.subtract(paid).max(BigDecimal.ZERO);

            String customerName = customerNameById.getOrDefault(o.getCustomerId(), "Unknown");

            ActionItem a = new ActionItem();
            a.setRuleCode(ruleCode());
            a.setCategory(cat);
            a.setSeverity(sev);
            a.setOwnerRole(ownerRole());
            a.setTitle(String.format("AR overdue — %s · %s (%d days)",
                    customerName, o.getCode(), days));
            a.setDescription(String.format(
                    "Order %s is %d days overdue. Outstanding %s %s (paid %s of %s). Due %s.",
                    o.getCode(), days, o.getCurrency(),
                    outstanding.toPlainString(),
                    paid.toPlainString(),
                    total.toPlainString(),
                    o.getDueDate()));
            a.setRefType("order");
            a.setRefId(o.getId());
            a.setRefCode(o.getCode());
            a.setDueDate(today);          // 今天就该处理
            a.setDataSnapshot(String.format(
                    "{\"order_id\":%d,\"customer_id\":%d,\"customer_name\":\"%s\","
                            + "\"total\":%s,\"paid\":%s,\"outstanding\":%s,"
                            + "\"due_date\":\"%s\",\"days_overdue\":%d,\"currency\":\"%s\"}",
                    o.getId(), o.getCustomerId(),
                    customerName.replace("\"", "\\\""),
                    total.toPlainString(), paid.toPlainString(), outstanding.toPlainString(),
                    o.getDueDate(), days, o.getCurrency()));
            out.add(a);
        }

        log.info("[Rule R-AR-01] overdue orders triggered = {} (1-7d: {}, 8-30d: {}, 31+d: {})",
                out.size(),
                out.stream().filter(a -> "low".equals(a.getSeverity())).count(),
                out.stream().filter(a -> "medium".equals(a.getSeverity())).count(),
                out.stream().filter(a -> "high".equals(a.getSeverity())).count());

        return out;
    }
}
