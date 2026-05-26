package ai.toafrica.agrios.ops.service.rule;

import ai.toafrica.agrios.ops.entity.ActionItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * R-PROD-01 农药安全间隔期阻断规则 (STUB).
 *
 * TODO: 需要 input.phi_days 字段 + activity.input_id (Sprint 11 投入品台账) 才能实施.
 * 当前阶段 evaluate() 返回空 list, 仅占位让 Spring 注入到引擎里.
 */
@Slf4j
@Component
public class PhiBlockRule implements ActionRule {

    @Override public String ruleCode()  { return "R-PROD-01"; }
    @Override public String category()  { return "pause"; }
    @Override public String severity()  { return "high"; }
    @Override public String ownerRole() { return "production"; }

    @Override
    public List<ActionItem> evaluate() {
        // TODO[Sprint 11+]: 接入 activity (spray) + input.phi_days
        //   - 对每个 plot 最近一次 spray 的 input.phi_days
        //   - 若 today < spray_date + phi_days → 触发, 禁止采收
        return Collections.emptyList();
    }
}
