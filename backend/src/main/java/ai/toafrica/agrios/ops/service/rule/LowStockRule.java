package ai.toafrica.agrios.ops.service.rule;

import ai.toafrica.agrios.ops.entity.ActionItem;
import ai.toafrica.agrios.warehouse.mapper.WarehouseReportMapper;
import ai.toafrica.agrios.warehouse.vo.WarehouseReportVO.LowStockItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * R-INV-04 Low stock rule (Sprint 22.9c).
 *
 *   Trigger: input_stock.qty_available < input_item.min_stock_qty
 *
 *   Severity:
 *     qty_available <= 0                                   high   (out of stock)
 *     shortage_qty / min_stock_qty >= 0.5                  high   (below half safety stock)
 *     shortage_qty > 0                                     medium
 *
 *   Action: trigger procurement / transfer / urgent restock
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LowStockRule implements ActionRule {

    private final WarehouseReportMapper reportMapper;

    @Override public String ruleCode()  { return "R-INV-04"; }
    @Override public String category()  { return "today"; }
    @Override public String severity()  { return "medium"; }
    @Override public String ownerRole() { return "warehouse"; }

    @Override
    public List<ActionItem> evaluate() {
        List<LowStockItem> rows = reportMapper.lowStockItems();
        if (rows.isEmpty()) return List.of();

        LocalDate today = LocalDate.now();
        List<ActionItem> out = new ArrayList<>();
        for (LowStockItem s : rows) {
            BigDecimal minQty = s.getMinStockQty();
            BigDecimal avail = s.getQtyAvailable() != null ? s.getQtyAvailable() : BigDecimal.ZERO;
            BigDecimal shortage = s.getShortageQty() != null ? s.getShortageQty() : BigDecimal.ZERO;
            if (shortage.signum() <= 0) continue;

            // Compute severity
            String sev;
            if (avail.signum() <= 0) {
                sev = "high"; // out of stock
            } else if (minQty != null && minQty.signum() > 0) {
                BigDecimal ratio = shortage.divide(minQty, 4, RoundingMode.HALF_UP);
                sev = (ratio.compareTo(new BigDecimal("0.5")) >= 0) ? "high" : "medium";
            } else {
                sev = "medium";
            }

            String availStr = avail.stripTrailingZeros().toPlainString();
            String minStr = minQty != null ? minQty.stripTrailingZeros().toPlainString() : "0";
            String shortStr = shortage.stripTrailingZeros().toPlainString();

            ActionItem a = new ActionItem();
            a.setRuleCode(ruleCode());
            a.setCategory("today");
            a.setSeverity(sev);
            a.setOwnerRole(ownerRole());
            a.setTitle(String.format("Low stock - %s @ %s - %s %s available (min %s, short %s)",
                    s.getInputItemCode(), s.getWarehouseName(),
                    availStr, s.getUnit() != null ? s.getUnit() : "",
                    minStr, shortStr));
            a.setDescription(String.format(
                    "Item %s (%s) at warehouse %s is below safety stock threshold. " +
                    "Current available: %s %s | Min stock: %s | Shortage: %s. " +
                    "Suggested action: create purchase order or transfer from another warehouse.",
                    s.getInputItemCode(),
                    s.getInputItemName() != null ? s.getInputItemName() : "",
                    s.getWarehouseName() != null ? s.getWarehouseName() : "",
                    availStr,
                    s.getUnit() != null ? s.getUnit() : "",
                    minStr,
                    shortStr));
            a.setRefType("input_stock");
            a.setRefId(s.getInputItemId());
            a.setRefCode(s.getInputItemCode());
            a.setDueDate(today);
            a.setDataSnapshot(String.format(
                    "{\"input_item_id\":%d,\"input_item_code\":\"%s\",\"warehouse_id\":%d,\"warehouse_name\":\"%s\",\"qty_available\":%s,\"min_stock_qty\":%s,\"shortage_qty\":%s}",
                    s.getInputItemId(),
                    s.getInputItemCode() != null ? s.getInputItemCode() : "",
                    s.getWarehouseId(),
                    s.getWarehouseName() != null ? s.getWarehouseName().replace("\"", "\\\"") : "",
                    avail.toPlainString(),
                    minStr,
                    shortage.toPlainString()));
            out.add(a);
        }

        if (!out.isEmpty()) {
            log.info("[Rule R-INV-04] low stock items triggered = {}", out.size());
        }
        return out;
    }
}
