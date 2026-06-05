package ai.toafrica.agrios.workflow.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Sprint 52 -- workflow template.
 *
 * <p>{@code schemaJson} stores the DSL described in PRD-HR-ADMIN-LEGAL-
 * WORKFLOW-v0.2 § 3.1. Parsed by {@code WorkflowSchemaParser} into a
 * {@code WorkflowSchema} record.</p>
 */
@Data
@TableName("wf_definition")
public class WfDefinition {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;
    private String name;
    private String module;
    private Integer version;
    private Integer active;
    /** JSON DSL: {@code { trigger, steps[] }}. */
    private String schemaJson;
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    private Long updatedBy;
}
