package ai.toafrica.agrios.ops.service.rule;

import ai.toafrica.agrios.ops.entity.ActionItem;
import ai.toafrica.agrios.packhouse.entity.Inventory;
import ai.toafrica.agrios.packhouse.entity.Sku;
import ai.toafrica.agrios.packhouse.mapper.InventoryMapper;
import ai.toafrica.agrios.packhouse.mapper.SkuMapper;
import ai.toafrica.agrios.production.entity.Batch;
import ai.toafrica.agrios.production.mapper.BatchMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * R-INV-05 Near-expiry rule (Sprint 26 / FEFO).
 *
 * Threshold: any inventory row with status=normal, qty_avail&gt;0, expiry_date set,
 *            and days_to_expiry between 0 and 3 (inclusive) triggers.
 *
 * Rows already past expiry (days_to_expiry &lt; 0) also trigger — they should
 * be scrapped or flagged urgently.
 *
 *   tab        = today
 *   severity   = high (≤3d) / urgent (already expired)
 *   owner_role = sales
 *   ref        = inventory.id
 *
 * Distinct from R-INV-01 (aging by prod_date): this rule uses
 * the resolved expiry date that came from variety/crop shelf life.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NearExpiryRule implements ActionRule {

    /** Days-to-expiry threshold — alert when ≤ this value (and ≥ 0). */
    private static final long NEAR_EXPIRY_THRESHOLD = 3L;

    private final InventoryMapper inventoryMapper;
    private final SkuMapper       skuMapper;
    private final BatchMapper     batchMapper;

    @Override public String ruleCode()  { return "R-INV-05"; }
    @Override public String category()  { return "today"; }
    @Override public String severity()  { return "high"; }
    @Override public String ownerRole() { return "sales"; }

    @Override
    public List<ActionItem> evaluate() {
        LocalDate today = LocalDate.now();

        List<Inventory> rows = inventoryMapper.selectList(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getStatus, "normal")
                        .gt(Inventory::getQtyAvail, 0)
                        .isNotNull(Inventory::getExpiryDate));

        List<ActionItem> out = new ArrayList<>();
        for (Inventory inv : rows) {
            LocalDate expiry = inv.getExpiryDate();
            if (expiry == null) continue;
            long daysToExpiry = ChronoUnit.DAYS.between(today, expiry);
            if (daysToExpiry > NEAR_EXPIRY_THRESHOLD) continue;

            Sku   sku   = skuMapper.selectById(inv.getSkuId());
            Batch batch = inv.getBatchId() == null ? null : batchMapper.selectById(inv.getBatchId());

            String skuName   = sku   == null ? ("SKU#" + inv.getSkuId())     : sku.getName();
            String batchCode = batch == null ? ("BATCH#" + inv.getBatchId()) : batch.getCode();

            boolean expired = daysToExpiry < 0;
            String title;
            if (expired) {
                title = String.format("EXPIRED — %s (%s units, %d days past expiry)",
                        skuName, plain(inv.getQtyAvail()), Math.abs(daysToExpiry));
            } else if (daysToExpiry == 0) {
                title = String.format("Expires today — %s (%s units)",
                        skuName, plain(inv.getQtyAvail()));
            } else {
                title = String.format("Near-expiry — %s (%s units, %d day%s left)",
                        skuName, plain(inv.getQtyAvail()),
                        daysToExpiry, daysToExpiry == 1 ? "" : "s");
            }

            ActionItem a = new ActionItem();
            a.setRuleCode(ruleCode());
            a.setCategory(category());
            a.setSeverity(expired ? "urgent" : severity());
            a.setOwnerRole(ownerRole());
            a.setTitle(title);
            a.setDescription(String.format(
                    "Batch %s, expiry %s. %s",
                    batchCode, expiry,
                    expired
                        ? "Scrap or quarantine immediately."
                        : "Prioritise this batch for picking (FEFO) or discount sale."));
            a.setRefType("inventory");
            a.setRefId(inv.getId());
            a.setRefCode(batchCode);
            a.setDueDate(today);
            a.setDataSnapshot(String.format(
                    "{\"sku_id\":%d,\"qty_avail\":\"%s\",\"expiry_date\":\"%s\",\"days_to_expiry\":%d}",
                    inv.getSkuId(), plain(inv.getQtyAvail()), expiry, daysToExpiry));
            out.add(a);
        }
        if (!out.isEmpty()) {
            log.info("[Rule R-INV-05] near-expiry / expired inventory rows triggered = {}", out.size());
        }
        return out;
    }

    private static String plain(java.math.BigDecimal v) {
        return v == null ? "0" : v.stripTrailingZeros().toPlainString();
    }
}
