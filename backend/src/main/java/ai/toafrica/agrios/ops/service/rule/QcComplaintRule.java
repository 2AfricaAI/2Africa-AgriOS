package ai.toafrica.agrios.ops.service.rule;

import ai.toafrica.agrios.ops.entity.ActionItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * R-QC-01 质量投诉规则 (STUB).
 *
 * TODO: 需要 qc_complaint 表 (Sprint 14 品控模块) 才能实施.
 * 当前阶段 evaluate() 返回空 list, 仅占位让 Spring 注入到引擎里.
 */
@Slf4j
@Component
public class QcComplaintRule implements ActionRule {

    @Override public String ruleCode()  { return "R-QC-01"; }
    @Override public String category()  { return "today"; }
    @Override public String severity()  { return "high"; }
    @Override public String ownerRole() { return "qc"; }

    @Override
    public List<ActionItem> evaluate() {
        // TODO[Sprint 14+]: 接入 qc_complaint 表
        //   - status=open 的投诉, 按 customer & batch 维度触发
        //   - 严重等级 (recall/major) 升级为 high, minor 为 medium
        return Collections.emptyList();
    }
}
