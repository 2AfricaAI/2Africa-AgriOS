package ai.toafrica.agrios.production.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "harvest_record", autoResultMap = true)
public class HarvestRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** HV-yyyyMMdd-NNN */
    private String code;
    private String clientUuid;

    private Long plotId;
    private Long planId;
    private Long cropId;
    private Long varietyId;

    /** 自动生成关联的 batch.id */
    private Long batchId;

    private LocalDate harvestDate;
    private BigDecimal qtyKg;

    /** GPS lat,lng - captured by mobile worker app (Sprint 20.5) */
    private String locationGps;

    private Long operatorId;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> photos;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;
    @TableField(fill = FieldFill.INSE