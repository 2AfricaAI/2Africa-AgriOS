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
 * R-PROD-04 计划逾期未采收 - Sprint 19.4 (NEW).
 *
 *   触发条件:
 *     - plan_harvest_date < today
 *     - status IN (planned, in_progress) (即未真正采收)
 *
 *   severity 按超期天数:
 *     1-3 days   medium
 *     4-7 days   high
 *     8+ days    high  + 标题强调
 *
 *   过期未采可能导致: 鲜度下降, 品级降, 卖不上价 → 损失.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HarvestOverdueRule implements ActionRule {

    private final PlantingPlanMapper planMapper;
    private final PlotMapper plotMapper;

    @Override public String ruleCode()  { return "R-PROD-04"; }
    @Override public String category()  { return "today"; }
    @Override public String severity()  { return "high"; }
    @Override public String ownerRole() { return "production"; }

    @Override
    public List<ActionItem> evaluate() {
        LocalDate today = LocalDate.now();

        List<PlantingPlan> plans = planMapper.selectList(
                new LambdaQueryWrapper<PlantingPlan>()
                        .in(PlantingPlan::getStatus, "planned", "in_progress")
                        .isNotNull(PlantingPlan::getPlanHarvestDate)
                        .lt(PlantingPlan::getPlanHarvestDate, today)
                        .orderByAsc(PlantingPlan::getPlanHarvestDate));

        if (plans.isEmpty()) return List.of();

        Map<Long, String> plotNameCache = new HashMap<>();
        List<ActionItem> out = new ArrayList<>();
        for (PlantingPlan p : plans) {
            long days = ChronoUnit.DAYS.between(p.getPlanHarvestDate(), today);
            if (days <= 0) continue;

            String sev = days <= 3 ? "medium" : "high";

            String plotName = plotNameCache.computeIfAbsent(p.getPlotId(), id -> {
                Plot plot = plotMapper.selectById(id);
                return plot != null ? plot.getName() : "Plot#" + id;
            });

            String titlePrefix = days >= 8 ? "🚨 Harvest overdue " : "Harvest overdue ";

            ActionItem a = new ActionItem();
            a.setRuleCode(ruleCode());
            a.setCategory("today");
            a.setSeverity(sev);
            a.setOwnerRole(ownerRole());
            a.setTitle(String.format("%s%d day%s — %s · %s",
                    titlePrefix, days, days == 1 ? "" : "s", plotName, p.getCode()));
            a.setDescription(String.format(
                    "Plan %s (plot %s) was scheduled to harvest on %s but is still in '%s' status — " +
                    "%d days overdue. Risk: freshness loss, grade downgrade, lower price. " +
                    "Action: harvest immediately or mark plan as completed if already done.",
                    p.getCode(), plotName, p.getPlanHarvestDate(), p.getStatus(), days));
            a.setRefType("planting_plan");
            a.setRefId(p.getId());
            a.setRefCode(p.getCode());
            a.setDueDate(today);
            a.setDataSnapshot(String.format(
                    "{\"plan_id\":%d,\"plot_id\":%d,\"plot_name\":\"%s\",\"days_overdue\":%d,\"plan_harvest_date\":\"%s\",\"status\":\"%s\"}",
                    p.getId(), p.getPlotId(),
                    plotName.replace("\"", "\\\""),
                    days, p.getPlanHarvestDate(), p.getStatus()));
            out.add(a);
        }

        log.info("[Rule R-PROD-04] overdue plans triggered = {} (<=3d: {}, 4+d: {})",
                out.size(),
                out.stream().filter(a -> "medium".equals(a.getSeverity())).count(),
                out.stream().filter(a -> "high".equals(a.getSeverity())).count());

        return out;
    }
}
