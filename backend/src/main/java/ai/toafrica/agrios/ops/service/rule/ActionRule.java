package ai.toafrica.agrios.ops.service.rule;

import ai.toafrica.agrios.ops.entity.ActionItem;

import java.util.List;

/**
 * 经营行动规则接口 — Sprint 10 规则引擎.
 *
 * 每条规则是一个 Spring Bean (@Component), 自动被 {@link ai.toafrica.agrios.ops.service.ActionEngineService}
 * 注入到 List&lt;ActionRule&gt; 里。
 *
 * 实现注意:
 *   - {@link #evaluate()} 必须是只读的, 不写库; 它返回的 ActionItem 列表交给引擎做 upsert.
 *   - 同一个规则在同一资源上多次评估应是 idempotent (依赖 rule_code+ref_type+ref_id 唯一键).
 *   - 元信息 (category / severity / ownerRole) 用 default 给出, 也方便监控/UI 分组.
 */
public interface ActionRule {

    /** 规则编码, 与 ActionItem.rule_code 对齐, 如 R-INV-01 */
    String ruleCode();

    /** today / week_risk / followup / pause - 决定显示在哪个 tab */
    String category();

    /** high / medium / low */
    String severity();

    /** sales / packhouse / finance / ceo / production / qc */
    String ownerRole();

    /**
     * 评估当前业务数据, 返回该规则当下需要"亮灯"的全部 ActionItem.
     *
     * 返回空 list = 没有触发 (引擎会把之前的 open 项自动转 auto_resolved).
     */
    List<ActionItem> evaluate();
}
