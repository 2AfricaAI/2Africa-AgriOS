package ai.toafrica.agrios.ops.service.rule;

import ai.toafrica.agrios.ops.entity.ActionItem;
import ai.toafrica.agrios.production.entity.HarvestRecord;
import ai.toafrica.agrios.production.entity.PlantingPlan;
import ai.toafrica.agrios.production.mapper.HarvestRecordMapper;
import ai.toafrica.agrios.production.mapper.PlantingPlanMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * R-INV-02 High loss rule - Sprint 19.5 (real implementation).
 *
 *   Without dedicated loss_record table, use proxy:
 *     harvest_record.qty_kg vs sum(batch.qty_kg) for the same harvest_record_id
 *     loss = (harvest_qty - batch_qty) / harvest_qty
 *
 *   Trigger (last 14 days of harvests):
 *     loss ratio > 20% -> trigger, severity by ratio
 *
 *   Large delta usually means:
 *     - Damage report / grading culls
 *     - Inaccurate weighing
 *     - Data entry error
 *
 *   Severity:
 *     >= 40%  high
 *     >= 30%  medium
 *     >= 20%  low
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HighLossRule implements ActionRule {

    private static final BigDecimal LOW_THRESHOLD    = new BigDecimal("0.20");
    private static final BigDecimal MEDIUM_THRESHOLD = new BigDecimal("0.30");
    private static final BigDecimal HIGH_THRESHOLD   = new BigDecimal("0.40");
    private static final int LOOKBACK_DAYS = 14;

    private final HarvestRecordMapper harvestMapper;
    private final PlantingPlanMapper planMapper;
    private final HarvestBatchSumProvider batchSumProvider;

    @Override public String ruleCode()  { return "R-INV-02"; }
    @Override public String category()  { return "today"; }
    @Override public String severity()  { return "medium"; }
    @Override public String ownerRole() { return "packhouse"; }

    @Override
    public List<ActionItem> evaluate() {
        LocalDate today = LocalDate.now();
        LocalDate since = today.minusDays(LOOKBACK_DAYS);

        List<HarvestRecord> harvests = harvestMapper.selectList(
                new LambdaQueryWrapper<HarvestRecord>()
                        .ge(HarvestRecord::getHarvestDate, since)
                        .isNotNull(HarvestRecord::getQtyKg));

        if (harvests.isEmpty()) return List.of();

        List<Long> harvestIds = harvests.stream().map(HarvestRecord::getId).toList();
        Map<Long, BigDecimal> batchSumByHarvest = batchSumProvider.sumQtyByHarvestRecord(harvestIds);

        Map<Long, String> planCodeCache = new HashMap<>();
        List<ActionItem> out = new ArrayList<>();
        for (HarvestRecord h : harvests) {
            BigDecimal harvestQty = h.getQtyKg();
            if (harvestQty == null || harvestQty.signum() <= 0) continue;

            BigDecimal batchSum = batchSumByHarvest.getOrDefault(h.getId(), BigDecimal.ZERO);
            BigDecimal loss = harvestQty.subtract(batchSum);
            if (loss.signum() <= 0) continue;

            BigDecimal ratio = loss.divide(harvestQty, 4, RoundingMode.HALF_UP);
            if (ratio.compareTo(LOW_THRESHOLD) < 0) continue;

            String sev;
            if (ratio.compareTo(HIGH_THRESHOLD) >= 0)        sev = "high";
            else if (ratio.compareTo(MEDIUM_THRESHOLD) >= 0) sev = "medium";
            else                                              sev = "low";

            String planCode = planCodeCache.computeIfAbsent(h.getPlanId(), id -> {
                PlantingPlan p = planMapper.selectById(id);
                return p != null ? p.getCode() : "Plan#" + id;
            });

            ActionItem a = new ActionItem();
            a.setRuleCode(ruleCode());
            a.setCategory("today");
            a.setSeverity(sev);
            a.setOwnerRole(ownerRole());
            a.setTitle(String.format("High loss - %s - harvest %s kg -> batch %s kg (%s%% loss)",
                    planCode,
                    harvestQty.stripTrailingZeros().toPlainString(),
                    batchSum.stripTrailingZeros().toPlainString(),
                    ratio.multiply(new BigDecimal(100)).setScale(1, RoundingMode.HALF_UP).toPlainString()));
            a.setDescription(String.format(
                    "Harvest record on %s (plan %s) shows %s kg harvested but only %s kg made it to batch - " +
                    "loss %s kg (%s%%). Check: weighing accuracy, grading culls, or data entry error.",
                    h.getHarvestDate(), planCode,
                    harvestQty.stripTrailingZeros().toPlainString(),
                    batchSum.stripTrailingZeros().toPlainString(),
                    loss.stripTrailingZeros().toPlainString(),
                    ratio.multiply(new BigDecimal(100)).setScale(1, RoundingMode.HALF_UP).toPlainString()));
            a.setRefType("harvest_record");
            a.setRefId(h.getId());
            a.setRefCode("HR-" + h.getId());
            a.setDueDate(today);
            a.setDataSnapshot(String.format(
                    "{\"harvest_id\":%d,\"plan_id\":%d,\"plan_code\":\"%s\",\"harvest_qty\":%s,\"batch_qty\":%s,\"loss\":%s,\"loss_ratio\":%s}",
                    h.getId(), h.getPlanId(),
                    planCode.replace("\"", "\\\""),
                    harvestQty.toPlainString(), batchSum.toPlainString(),
                    loss.toPlainString(), ratio.toPlainString()));
            out.add(a);
        }

        if (!out.isEmpty()) {
            log.info("[Rule R-INV-02] high loss harvests triggered = {} (>= 20% loss in last {} days)",
                    out.size(), LOOKBACK_DAYS);
        }
        return out;
    }

    public interface HarvestBatchSumProvider {
        Map<Long, BigDecimal> sumQtyByHarvestRecord(@Param("ids") List<Long> harvestIds);
    }
}
