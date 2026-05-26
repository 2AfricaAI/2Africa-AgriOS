package ai.toafrica.agrios.ops.service.rule;

import ai.toafrica.agrios.ops.entity.ActionItem;
import ai.toafrica.agrios.procurement.entity.PurchaseOrder;
import ai.toafrica.agrios.procurement.entity.Supplier;
import ai.toafrica.agrios.procurement.mapper.PurchaseOrderMapper;
import ai.toafrica.agrios.procurement.mapper.SupplierMapper;
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
 * R-AP-01 应付逾期规则 - Sprint 17.5 (镜像 R-AR-01).
 *
 *   扫描所有 purchase_order:
 *     status NOT IN (cancelled)
 *     payment_status <> 'paid'
 *     due_date < today
 *
 *   每个命中的 PO 生成 ActionItem (ref_type=purchase_order, ref_id=po.id):
 *     1-7   天逾期 → severity=low,    category=followup
 *     8-30  天逾期 → severity=medium, category=today
 *     31+   天逾期 → severity=high,   category=today
 *
 *   不付供应商会断供, 比 AR 还紧迫.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueApRule implements ActionRule {

    private final PurchaseOrderMapper orderMapper;
    private final SupplierMapper supplierMapper;

    @Override public String ruleCode()  { return "R-AP-01"; }
    @Override public String category()  { return "today"; }
    @Override public String severity()  { return "high"; }
    @Override public String ownerRole() { return "finance"; }

    @Override
    public List<ActionItem> evaluate() {
        LocalDate today = LocalDate.now();

        List<PurchaseOrder> overdue = orderMapper.selectList(
                new LambdaQueryWrapper<PurchaseOrder>()
                        .notIn(PurchaseOrder::getStatus, "cancelled")
                        .ne(PurchaseOrder::getPaymentStatus, "paid")
                        .isNotNull(PurchaseOrder::getDueDate)
                        .lt(PurchaseOrder::getDueDate, today)
                        .orderByAsc(PurchaseOrder::getDueDate));

        if (overdue.isEmpty()) {
            return List.of();
        }

        // 批量取供应商名 (避免 N+1)
        Map<Long, String> supplierNameById = new HashMap<>();
        for (PurchaseOrder o : overdue) {
            supplierNameById.computeIfAbsent(o.getSupplierId(), id -> {
                Supplier s = supplierMapper.selectById(id);
                return s != null ? s.getName() : "Unknown";
            });
        }

        List<ActionItem> out = new ArrayList<>();
        for (PurchaseOrder o : overdue) {
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

            String supplierName = supplierNameById.getOrDefault(o.getSupplierId(), "Unknown");

            ActionItem a = new ActionItem();
            a.setRuleCode(ruleCode());
            a.setCategory(cat);
            a.setSeverity(sev);
            a.setOwnerRole(ownerRole());
            a.setTitle(String.format("AP overdue — %s · %s (%d days)",
                    supplierName, o.getCode(), days));
            a.setDescription(String.format(
                    "PO %s to supplier %s is %d days overdue. Outstanding %s %s (paid %s of %s). Due %s. " +
                    "Pay soon to avoid supply disruption.",
                    o.getCode(), supplierName, days, o.getCurrency(),
                    outstanding.toPlainString(),
                    paid.toPlainString(),
                    total.toPlainString(),
                    o.getDueDate()));
            a.setRefType("purchase_order");
            a.setRefId(o.getId());
            a.setRefCode(o.getCode());
            a.setDueDate(today);
            a.setDataSnapshot(String.format(
                    "{\"po_id\":%d,\"supplier_id\":%d,\"supplier_name\":\"%s\","
                            + "\"total\":%s,\"paid\":%s,\"outstanding\":%s,"
                            + "\"due_date\":\"%s\",\"days_overdue\":%d,\"currency\":\"%s\"}",
                    o.getId(), o.getSupplierId(),
                    supplierName.replace("\"", "\\\""),
                    total.toPlainString(), paid.toPlainString(), outstanding.toPlainString(),
                    o.getDueDate(), days, o.getCurrency()));
            out.add(a);
        }

        log.info("[Rule R-AP-01] overdue POs triggered = {} (1-7d: {}, 8-30d: {}, 31+d: {})",
                out.size(),
                out.stream().filter(a -> "low".equals(a.getSeverity())).count(),
                out.stream().filter(a -> "medium".equals(a.getSeverity())).count(),
                out.stream().filter(a -> "high".equals(a.getSeverity())).count());

        return out;
    }
}
