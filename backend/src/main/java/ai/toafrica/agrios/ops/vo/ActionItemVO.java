package ai.toafrica.agrios.ops.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ActionItem 列表 / 详情 VO. 字段与 entity 1:1, 但 createdBy 等内部字段被屏蔽.
 */
@Data
public class ActionItemVO {
    private Long          id;
    private String        ruleCode;
    private String        severity;
    private String        category;
    private String        title;
    private String        description;
    private String        ownerRole;
    private String        refType;
    private Long          refId;
    private String        refCode;
    private String        status;
    private LocalDate     dueDate;
    private String        dataSnapshot;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
    private Long          resolvedBy;
    private String        resolvedRemark;
}
