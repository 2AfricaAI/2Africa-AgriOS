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
 * R-INV-01 临期库存规则
 *
 * 阈值: prod_date 距今 > 14 天 → 触发. (MVP hardcode, 后续可移到 config / 表)
 *
 * 命中: 每一条 status=normal AND qty_avail>0 的 inventory.
 *   tab        = today
 *   severity   = high
 *   owner_role = sales
 *   ref        = inventory.id
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryAgingRule implements ActionRule {

    /** 临期阈值 - 天数 */
    private static final long AGING_DAYS = 14L;

    private final InventoryMapper inventoryMapper;
    private final SkuMapper       skuMapper;
    private final BatchMapper     batchMapper;

    @Override public String ruleCode()  { return "R-INV-01"; }
    @Override public String category()  { return "today"; }
    @Override public String severity()  { return "high"; }
    @Override public String ownerRole() { return "sales"; }

    @Override
    public List<ActionItem> evaluate() {
        LocalDate today = LocalDate.now();

        List<Inventory> rows = inventoryMapper.selectList(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getStatus, "normal")
                        .gt(Inventory::getQtyAvail, 0));

        List<ActionItem> out = new ArrayList<>();
        for (Inventory inv : rows) {
            LocalDate prodDate = inv.getProdDate();
            if (prodDate == null) continue;
            long daysOld = ChronoUnit.DAYS.between(prodDate, today);
            if (daysOld <= AGING_DAYS) continue;

            Sku   sku   = skuMapper.selectById(inv.getSkuId());
            Batch batch = inv.getBatchId() == null ? null : batchMapper.selectById(inv.getBatchId());

            String skuName   = sku   == null ? ("SKU#" + inv.getSkuId())   : sku.getName();
            String batchCode = batch == null ? ("BATCH#" + inv.getBatchId()) : batch.getCode();

            ActionItem a = new ActionItem();
            a.setRuleCode(ruleCode());
            a.setCategory(category());
            a.setSeverity(severity());
            a.setOwnerRole(ownerRole());
            a.setTitle(String.format("Aging inventory — %s (%s units, %d days old)",
                    skuName, plain(inv.getQtyAvail()), daysOld));
            a.setDescription(String.format(
                    "From batch %s, produced %s. Suggest urgent sale.",
                    batchCode, prodDate));
            a.setRefType("inventory");
            a.setRefId(inv.getId());
            a.setRefCode(batchCode);
            a.setDueDate(today);
            a.setDataSnapshot(String.format(
                    "{\"sku_id\":%d,\"qty_avail\":\"%s\",\"prod_date\":\"%s\",\"days_old\":%d}",
                    inv.getSkuId(), plain(inv.getQtyAvail()), prodDate, daysOld));
            out.add(a);
        }
        if (!out.isEmpty()) {
            log.info("[Rule R-INV-01] aging inventory rows triggered = {}", out.size());
        }
        return out;
    }

    private static String plain(java.math.BigDecimal v) {
        return v == null ? "0" : v.stripTrailingZeros().toPlainString();
    }
}
