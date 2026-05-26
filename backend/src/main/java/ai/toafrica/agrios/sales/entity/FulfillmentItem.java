package ai.toafrica.agrios.sales.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("fulfillment_item")
public class FulfillmentItem {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long fulfillmentId;
    private Long orderItemId;
    private Long inventoryId;
    /** 冗余, 加速追溯 */
    private Long batchId;
    private Long skuId;

    private BigDecimal qty;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
