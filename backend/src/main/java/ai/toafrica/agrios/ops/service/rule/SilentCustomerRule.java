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

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * R-CUST-01 沉默客户规则
 *
 * 对每个 status=active 的客户:
 *   找最近一次未取消的销售订单, 取 order_date
 *   如果 >30 天前 (或从未下过单 且建档 >30 天) → 触发
 *
 *   tab        = followup
 *   severity   = low
 *   owner_role = sales
 *   ref        = customer.id
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SilentCustomerRule implements ActionRule {

    /** 沉默阈值 - 天数 */
    private static final long SILENT_DAYS = 30L;

    private final CustomerMapper   customerMapper;
    private final SalesOrderMapper orderMapper;

    @Override public String ruleCode()  { return "R-CUST-01"; }
    @Override public String category()  { return "followup"; }
    @Override public String severity()  { return "low"; }
    @Override public String ownerRole() { return "sales"; }

    @Override
    public List<ActionItem> evaluate() {
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.minusDays(SILENT_DAYS);

        List<Customer> actives = customerMapper.selectList(
                new LambdaQueryWrapper<Customer>()
                        .eq(Customer::getStatus, "active"));

        List<ActionItem> out = new ArrayList<>();
        for (Customer c : actives) {
            // 最近一次非 cancelled 的订单
            List<SalesOrder> orders = orderMapper.selectList(
                    new LambdaQueryWrapper<SalesOrder>()
                            .eq(SalesOrder::getCustomerId, c.getId())
                            .ne(SalesOrder::getStatus, "cancelled")
                            .orderByDesc(SalesOrder::getOrderDate)
                            .last("LIMIT 1"));

            LocalDate lastOrderDate = orders.isEmpty() ? null : orders.get(0).getOrderDate();

            // 从未下过单 → 用 sinceDate 当基线
            LocalDate baseline = lastOrderDate != null ? lastOrderDate : c.getSinceDate();
            if (baseline == null) continue;        // 既无订单又无 sinceDate, 数据不全, 跳过
            if (!baseline.isBefore(cutoff)) continue;

            long days = ChronoUnit.DAYS.between(baseline, today);

            ActionItem a = new ActionItem();
            a.setRuleCode(ruleCode());
            a.setCategory(category());
            a.setSeverity(severity());
            a.setOwnerRole(ownerRole());
            a.setTitle(String.format("Silent customer — %s", c.getName()));
            a.setDescription(lastOrderDate == null
                    ? String.format("No order ever placed (%d days since onboarding). Suggest follow-up call.", days)
                    : String.format("No order in last %d days. Suggest follow-up call.", days));
            a.setRefType("customer");
            a.setRefId(c.getId());
            a.setRefCode(c.getCode());
            a.setDueDate(today.plusDays(3));
            a.setDataSnapshot(String.format(
                    "{\"customer_id\":%d,\"last_order_date\":%s,\"days_silent\":%d}",
                    c.getId(),
                    lastOrderDate == null ? "null" : "\"" + lastOrderDate + "\"",
                    days));
            out.add(a);
        }
        if (!out.isEmpty()) {
            log.info("[Rule R-CUST-01] silent customers triggered = {}", out.size());
        }
        return out;
    }
}
