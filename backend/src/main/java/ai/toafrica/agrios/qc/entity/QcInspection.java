package ai.toafrica.agrios.qc.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "qc_inspection", autoResultMap = true)
public class QcInspection {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;
    private String inspectionType;
    private String refType;
    private Long refId;
    private String refCode;
    private LocalDate inspectDate;
    private Long inspectorId;
    private String result;
    private String resultRemark;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> photoIds;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
