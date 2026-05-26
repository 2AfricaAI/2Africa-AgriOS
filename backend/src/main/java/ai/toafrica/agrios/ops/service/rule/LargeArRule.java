package ai.toafrica.agrios.ops.service.rule;

import ai.toafrica.agrios.ops.entity.ActionItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * R-AR-02 大额应收规则 (STUB).
 *
 * TODO: 需要 payment 表 (Sprint 12 应收应付模块) 才能实施.
 * 当前阶段 evaluate() 返回空 list, 仅占位让 Spring 注入到引擎里.
 */
@Slf4j
@Component
public class LargeArRule implements ActionRule {

    @Override public String ruleCode()  { return "R-AR-02"; }
    @Override public String category()  { return "week_risk"; }
    @Override public String severity()  { return "medium"; }
    @Override public String ownerRole() { return "finance"; }

    @Override
    public List<ActionItem> evaluate() {
        // TODO[Sprint 12+]: 接入 payment 表
        //   - 单个 customer 应收 > 阈值 (按 currency 配置, 如 KES 500k)
        //   - 触发 medium severity, 提醒商务跟进
        return Collections.emptyList();
    }
}
