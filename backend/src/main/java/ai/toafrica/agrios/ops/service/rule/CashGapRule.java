package ai.toafrica.agrios.ops.service.rule;

import ai.toafrica.agrios.ops.entity.ActionItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * R-CASH-01 现金缺口规则 (STUB).
 *
 * TODO: 需要 cash_flow_snapshot 表 (Sprint 13 现金流模块) 才能实施.
 * 当前阶段 evaluate() 返回空 list, 仅占位让 Spring 注入到引擎里.
 */
@Slf4j
@Component
public class CashGapRule implements ActionRule {

    @Override public String ruleCode()  { return "R-CASH-01"; }
    @Override public String category()  { return "week_risk"; }
    @Override public String severity()  { return "high"; }
    @Override public String ownerRole() { return "ceo"; }

    @Override
    public List<ActionItem> evaluate() {
        // TODO[Sprint 13+]: 接入 cash_flow_snapshot
        //   - 预测未来 7 / 14 天 入账 - 出账
        //   - 若净额 < 0 → 触发 high severity, owner = ceo
        return Collections.emptyList();
    }
}
