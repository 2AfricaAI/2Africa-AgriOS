package ai.toafrica.agrios.ops.service.rule;

import ai.toafrica.agrios.ops.entity.ActionItem;
import ai.toafrica.agrios.procurement.mapper.VendorPaymentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * R-AP-02 大额应付供应商集中度 - Sprint 19.2 (NEW, 镜像 R-AR-02).
 *
 *   单一供应商应付 outstanding > 阈值 → 触发, 提醒备款防断供.
 *   数据源: vendorPaymentMapper.apAgingBySupplier() (Sprint 17.5 已建).
 *
 *   阈值 (KES):
 *     >= 1,000,000 → high
 *     >= 500,000   → medium
 *     >= 200,000   → low
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LargeApRule implements ActionRule {

    private static final BigDecimal LOW_THRESHOLD    = new BigDecimal("200000");
    private static final BigDecimal MEDIUM_THRESHOLD = new BigDecimal("500000");
    private static final BigDecimal HIGH_THRESHOLD   = new BigDecimal("1000000");

    private final VendorPaymentMapper vendorPaymentMapper;

    @Override public String ruleCode()  { return "R-AP-02"; }
    @Override public String category()  { return "week_risk"; }
    @Override public String severity()  { return "medium"; }
    @Override public String ownerRole() { return "finance"; }

    @Override
    public List<ActionItem> evaluate() {
        List<Map<String, Object>> agingRows = vendorPaymentMapper.apAgingBySupplier();
        if (agingRows == null || agingRows.isEmpty()) return List.of();

        List<ActionItem> out = new ArrayList<>();
        for (Map<String, Object> r : agingRows) {
            BigDecimal outstanding = toBd(r.get("ap_outstanding"));
            if (outstanding.compareTo(LOW_THRESHOLD) < 0) continue;

            String sev;
            if (outstanding.compareTo(HIGH_THRESHOLD) >= 0)        sev = "high";
            else if (outstanding.compareTo(MEDIUM_THRESHOLD) >= 0) sev = "medium";
            else                                                    sev = "low";

            Long supplierId   = toLong(r.get("supplier_id"));
            String supplierCode = (String) r.get("supplier_code");
            String supplierName = (String) r.get("supplier_name");

            BigDecimal aging30 = toBd(r.get("aging_30_plus"));
            BigDecimal aging1530 = toBd(r.get("aging_15_30"));

            ActionItem a = new ActionItem();
            a.setRuleCode(ruleCode());
            a.setCategory("week_risk");
            a.setSeverity(sev);
            a.setOwnerRole(ownerRole());
            a.setTitle(String.format("Large AP — %s · %s KES payable",
                    supplierName, fmt(outstanding)));
            a.setDescription(String.format(
                    "Supplier %s has %s KES outstanding payable (15-30d: %s, 30+d: %s). " +
                    "Ensure cash reserves or contact supplier — late payment risks supply disruption.",
                    supplierName, fmt(outstanding), fmt(aging1530), fmt(aging30)));
            a.setRefType("supplier");
            a.setRefId(supplierId);
            a.setRefCode(supplierCode);
            a.setDueDate(java.time.LocalDate.now().plusDays(3));
            a.setDataSnapshot(String.format(
                    "{\"supplier_id\":%d,\"outstanding\":%s,\"aging_1530\":%s,\"aging_30plus\":%s}",
                    supplierId, outstanding.toPlainString(),
                    aging1530.toPlainString(), aging30.toPlainString()));
            out.add(a);
        }

        if (!out.isEmpty()) {
            log.info("[Rule R-AP-02] large AP suppliers triggered = {}", out.size());
        }
        return out;
    }

    private static BigDecimal toBd(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        return new BigDecimal(v.toString());
    }
    private static Long toLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Long l) return l;
        if (v instanceof Number n) return n.longValue();
        return Long.parseLong(v.toString());
    }
    private static String fmt(BigDecimal v) {
        return v == null ? "0" : v.toPlainString();
    }
}
