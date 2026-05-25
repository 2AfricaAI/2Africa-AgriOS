package ai.toafrica.agrios.production.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "activity", autoResultMap = true)
public class Activity {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 幂等键 (前端生成 UUID,后端去重) */
    private String clientUuid;

    private Long plotId;
    private Long planId;

    /** sow / fertilize / spray / weed / water / prune / other */
    private String activityType;

    private LocalDate occurDate;

    /** 实际操作人 (当前阶段用 sys_user.id; 等 staff 表上线后切换) */
    private Long operatorId;

    /** 照片 - JSON 列存 file IDs 数组,例如 [1,2,3] */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> photos;

    private String locationGps;
    private String remark;

    /** pending / approved / rejected */
    private String auditStatus;
    private Long auditorId;
    private LocalDateTime auditedAt;
    private String auditRemark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
