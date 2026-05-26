package ai.toafrica.agrios.sales.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("fulfillment")
public class Fulfillment {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** SH-yyyyMMdd-NNN */
    private String code;

    private Long orderId;
    private Long pickerId;

    private LocalDateTime planShipAt;
    private LocalDateTime shipAt;
    private LocalDateTime deliveredAt;

    /** pending / picking / ready / shipped / delivered / cancelled */
    private String status;

    /** self / logistics */
    private String shipMethod;
    private String trackNo;
    private String driverName;
    private String driverPhone;
    private String vehicleNo;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
