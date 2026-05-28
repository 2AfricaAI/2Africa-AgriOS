package ai.toafrica.agrios.ops.service.rule;

import ai.toafrica.agrios.ops.entity.ActionItem;
import ai.toafrica.agrios.production.entity.PlantingPlan;
import ai.toafrica.agrios.production.mapper.PlantingPlanMapper;
import ai.toafrica.agrios.qc.service.PhiCheckService;
import ai.toafrica.agrios.qc.vo.PhiCheckVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * R-PROD-01 PHI (Pre-Harvest Interval) block rule.
 *
 * Sprint 23 / Phase 5: real implementation (was stub before).
 *
 * 遍历所有 in_progress / planted 状态的 plan, 用 PhiCheckService 检查今天能否采收;
 * 如果不能(blocked) → 生成 action_item, severity 按剩余天数判定.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PhiBlockRule implements ActionRule {

    private final PlantingPlanMapper planMapper;
    private final PhiCheckService phiCheckService;

    @Override public String ruleCode()  { return "R-PROD-01"; }
    @Override public String category()  { return "pause"; }
    @Override public String severity()  { return "high"; }
    @Override public String ownerRole() { return "production"; }

    @Override
    public List<ActionItem> evaluate() {
        List<PlantingPlan> plans = planMapper.selectList(
                new LambdaQueryWrapper<PlantingPlan>()
                        .in(PlantingPlan::getStatus, "in_progress", "planted"));
        if (plans.isEmpty()) return List.of();

        LocalDate today = LocalDate.now();
        List<ActionItem> out = new ArrayList<>();
        for (PlantingPlan plan : plans) {
            PhiCheckVO phi = phiCheckService.checkPlan(plan.getId(), today);
            if (!phi.isBlocked()) continue;

            int days = phi.getDaysRemaining();
            String sev = (days >= 7) ? "high" : (days >= 3) ? "medium" : "low";

            var firstSpray = phi.getBlockingSprays().get(0);
            ActionItem a = new ActionItem();
            a.setRuleCode(ruleCode());
            a.setCategory("pause");
            a.setSeverity(sev);
            a.setOwnerRole(ownerRole());
            a.setTitle(String.format("PHI block: %s - %d days until %s",
                    plan.getCode(), days, phi.getEarliestSafeDate()));
            a.setDescription(String.format(
                    "Plan %s cannot be harvested until %s. " +
                    "Last spray of %s (active: %s, PHI %d days) was on %s. " +
                    "Wait %d more days.",
                    plan.getCode(),
                    phi.getEarliestSafeDate(),
                    firstSpray.getInputItemCode(),
                    firstSpray.getActiveIngredient() != null ? firstSpray.getActiveIngredient() : "n/a",
                    firstSpray.getPhiDays(),
                    firstSpray.getSprayDate(),
                    days));
            a.setRefType("planting_plan");
            a.setRefId(plan.getId());
            a.setRefCode(plan.getCode());
            a.setDueDate(phi.getEarliestSafeDate());
            a.setDataSnapshot(String.format(
                    "{\"plan_id\":%d,\"plan_code\":\"%s\",\"earliest_safe\":\"%s\",\"days_remaining\":%d,\"blocking_sprays\":%d}",
                    plan.getId(),
                    plan.getCode() != null ? plan.getCode().replace("\"", "\\\"") : "",
                    phi.getEarliestSafeDate(),
                    days,
                    phi.getBlockingSprays().size()));
            out.add(a);
        }

        if (!out.isEmpty()) {
            log.info("[Rule R-PROD-01] PHI-blocked plans = {}", out.size());
        }
        return out;
    }
}
