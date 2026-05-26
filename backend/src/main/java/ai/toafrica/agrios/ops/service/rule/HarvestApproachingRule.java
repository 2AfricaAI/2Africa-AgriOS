package ai.toafrica.agrios.ops.service.rule;

import ai.toafrica.agrios.ops.entity.ActionItem;
import ai.toafrica.agrios.production.entity.PlantingPlan;
import ai.toafrica.agrios.production.entity.Plot;
import ai.toafrica.agrios.production.mapper.PlantingPlanMapper;
import ai.toafrica.agrios.production.mapper.PlotMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * R-PROD-03 临采预警 - Sprint 19.3 (NEW).
 *
 *   触发条件:
 *     - planting_plan.plan_harvest_date 在 [today, today+7] 内
 *     - status IN (planned, in_progress) (未采收/未取消)
 *
 *   severity:
 *     <= 3 days  high   (今日必做, 备人备车)
 *     4-7 days   medium (本周风险, 提前规划)
 *
 *   提醒 packhouse 负责人 备齐人手/分级线/包装规格/物流车.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HarvestApproachingRule implements ActionRule {

    private final PlantingPlanMapper planMapper;
    private final PlotMapper plotMapper;

    @Override public String ruleCode()  { return "R-PROD-03"; }
    @Override public String category()  { return "today"; }
    @Override public String severity()  { return "medium"; }
    @Override public String ownerRole() { return "packhouse"; }

    @Override
    public List<ActionItem> evaluate() {
        LocalDate today = LocalDate.now();
        LocalDate horizon = today.plusDays(7);

        List<PlantingPlan> plans = planMapper.selectList(
                new LambdaQueryWrapper<PlantingPlan>()
                        .in(PlantingPlan::getStatus, "planned", "in_progress")
                        .isNotNull(PlantingPlan::getPlanHarvestDate)
                        .ge(PlantingPlan::getPlanHarvestDate, today)
                        .le(PlantingPlan::getPlanHarvestDate, horizon)
                        .orderByAsc(PlantingPlan::getPlanHarvestDate));

        if (plans.isEmpty()) return List.of();

        Map<Long, String> plotNameCache = new HashMap<>();
        List<ActionItem> out = new ArrayList<>();
        for (PlantingPlan p : plans) {
            long days = ChronoUnit.DAYS.between(today, p.getPlanHarvestDate());
            String sev = days <= 3 ? "high" : "medium";
            String cat = days <= 3 ? "today" : "week_risk";

            String plotName = plotNameCache.computeIfAbsent(p.getPlotId(), id -> {
                Plot plot = plotMapper.selectById(id);
                return plot != null ? plot.getName() : "Plot#" + id;
            });

            ActionItem a = new ActionItem();
            a.setRuleCode(ruleCode());
            a.setCategory(cat);
            a.setSeverity(sev);
            a.setOwnerRole(ownerRole());
            a.setTitle(String.format("Harvest in %d day%s — %s · %s",
                    days, days == 1 ? "" : "s", plotName, p.getCode()));
            a.setDescription(String.format(
                    "Plan %s (plot %s, area %s mu) expected harvest %s. " +
                    "Mobilize labor / grading line / packaging / transport in advance.",
                    p.getCode(), plotName,
                    p.getAreaMu() != null ? p.getAreaMu().toPlainString() : "?",
                    p.getPlanHarvestDate()));
            a.setRefType("planting_plan");
            a.setRefId(p.getId());
            a.setRefCode(p.getCode());
            a.setDueDate(p.getPlanHarvestDate());
            a.setDataSnapshot(String.format(
                    "{\"plan_id\":%d,\"plot_id\":%d,\"plot_name\":\"%s\",\"days_to_harvest\":%d,\"plan_harvest_date\":\"%s\"}",
                    p.getId(), p.getPlotId(),
                    plotName.replace("\"", "\\\""),
                    days, p.getPlanHarvestDate()));
            out.add(a);
        }

        log.info("[Rule R-PROD-03] approaching harvests triggered = {} (<=3d: {}, 4-7d: {})",
                out.size(),
                out.stream().filter(a -> "high".equals(a.getSeverity())).count(),
                out.stream().filter(a -> "medium".equals(a.getSeverity())).count());

        return out;
    }
}
