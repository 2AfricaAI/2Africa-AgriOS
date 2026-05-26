package ai.toafrica.agrios.ops.service.rule;

import ai.toafrica.agrios.ops.entity.ActionItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * R-INV-02 高损耗规则 (STUB).
 *
 * TODO: 需要 loss_record 表 (Sprint 11+ 包装/库存损耗台账) 才能实施.
 * 当前阶段 evaluate() 返回空 list, 仅占位让 Spring 注入到引擎里.
 */
@Slf4j
@Component
public class HighLossRule implements ActionRule {

    @Override public String ruleCode()  { return "R-INV-02"; }
    @Override public String category()  { return "today"; }
    @Override public String severity()  { return "high"; }
    @Override public String ownerRole() { return "packhouse"; }

    @Override
    public List<ActionItem> evaluate() {
        // TODO[Sprint 11+]: 接入 loss_record 表
        //   - 计算最近 7 天 loss_kg / packed_kg 比率 by batch / sku
        //   - 比率 > 5% → 触发 high severity
        return Collections.emptyList();
    }
}
