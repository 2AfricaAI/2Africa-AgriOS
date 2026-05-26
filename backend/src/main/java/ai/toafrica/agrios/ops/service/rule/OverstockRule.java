package ai.toafrica.agrios.ops.service.rule;

import ai.toafrica.agrios.ops.entity.ActionItem;
import ai.toafrica.agrios.packhouse.entity.Inventory;
import ai.toafrica.agrios.packhouse.entity.Sku;
import ai.toafrica.agrios.packhouse.mapper.InventoryMapper;
import ai.toafrica.agrios.packhouse.mapper.SkuMapper;
import ai.toafrica.agrios.sales.entity.Revenue;
import ai.toafrica.agrios.sales.mapper.RevenueMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * R-PROD-02 库存过高规则
 *
 * 算法 (按 SKU 维度):
 *   currentTotal = SUM(inventory.qty_avail WHERE status=normal AND sku_id=?)
 *   avgDailySold = SUM(revenue.qty WHERE recognition_date >= today-30d AND sku_id=?) / 30
 *   if avgDailySold == 0  → 跳过 (没销售数据, 无法判断)
 *   if currentTotal / avgDailySold > 21  → 触发 (相当于 3 周库存)
 *
 *   tab        = week_risk
 *   severity   = medium
 *   owner_role = production
 *   ref        = sku.id
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OverstockRule implements ActionRule {

    /** 触发阈值: 现有可用库存 / 日均销量 > 此天数 (即 3 周) */
    private static final int THRESHOLD_DAYS = 21;

    /** 销量统计回溯窗口 */
    private static final int SALES_WINDOW_DAYS = 30;

    private final InventoryMapper inventoryMapper;
    private final RevenueMapper   revenueMapper;
    private final SkuMapper       skuMapper;

    @Override public String ruleCode()  { return "R-PROD-02"; }
    @Override public String category()  { return "week_risk"; }
    @Override public String severity()  { return "medium"; }
    @Override public String ownerRole() { return "production"; }

    @Override
    public List<ActionItem> evaluate() {
        // 1) 聚合 inventory qty_avail by sku
        List<Inventory> invs = inventoryMapper.selectList(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getStatus, "normal")
                        .gt(Inventory::getQtyAvail, 0));
        Map<Long, BigDecimal> qtyBySku = new HashMap<>();
        for (Inventory inv : invs) {
            qtyBySku.merge(inv.getSkuId(), inv.getQtyAvail(), BigDecimal::add);
        }
        if (qtyBySku.isEmpty()) return List.of();

        // 2) 聚合最近 30 天 revenue.qty by sku
        LocalDate from = LocalDate.now().minusDays(SALES_WINDOW_DAYS);
        List<Revenue> recent = revenueMapper.selectList(
                new LambdaQueryWrapper<Revenue>()
                        .ge(Revenue::getRecognitionDate, from)
                        .eq(Revenue::getStatus, "recognized"));
        Map<Long, BigDecimal> soldBySku = new HashMap<>();
        for (Revenue r : recent) {
            if (r.getQty() == null) continue;
            soldBySku.merge(r.getSkuId(), r.getQty(), BigDecimal::add);
        }

        // 3) 比对 → ActionItem
        List<ActionItem> out = new ArrayList<>();
        LocalDate today = LocalDate.now();
        BigDecimal windowDays = BigDecimal.valueOf(SALES_WINDOW_DAYS);

        for (Map.Entry<Long, BigDecimal> e : qtyBySku.entrySet()) {
            Long       skuId   = e.getKey();
            BigDecimal current = e.getValue();
            BigDecimal sold30  = soldBySku.getOrDefault(skuId, BigDecimal.ZERO);
            if (sold30.signum() == 0) continue;          // 没销售数据, 跳过

            BigDecimal avgDaily = sold30.divide(windowDays, 4, RoundingMode.HALF_UP);
            BigDecimal ratio    = current.divide(avgDaily, 1, RoundingMode.HALF_UP);
            if (ratio.compareTo(BigDecimal.valueOf(THRESHOLD_DAYS)) <= 0) continue;

            Sku sku = skuMapper.selectById(skuId);
            String skuName = sku == null ? ("SKU#" + skuId) : sku.getName();
            String skuCode = sku == null ? null : sku.getCode();

            ActionItem a = new ActionItem();
            a.setRuleCode(ruleCode());
            a.setCategory(category());
            a.setSeverity(severity());
            a.setOwnerRole(ownerRole());
            a.setTitle(String.format("Overstock — %s (%s days of supply on hand)",
                    skuName, ratio.toPlainString()));
            a.setDescription("Consider delaying next harvest or running a promo.");
            a.setRefType("sku");
            a.setRefId(skuId);
            a.setRefCode(skuCode);
            a.setDueDate(today.plusDays(7));            // 周风险, 给一周时间处理
            a.setDataSnapshot(String.format(
                    "{\"sku_id\":%d,\"qty_avail_total\":\"%s\",\"sold_30d\":\"%s\",\"avg_daily\":\"%s\",\"days_supply\":\"%s\"}",
                    skuId, current.toPlainString(), sold30.toPlainString(),
                    avgDaily.toPlainString(), ratio.toPlainString()));
            out.add(a);
        }
        if (!out.isEmpty()) {
            log.info("[Rule R-PROD-02] overstock SKUs triggered = {}", out.size());
        }
        return out;
    }
}
