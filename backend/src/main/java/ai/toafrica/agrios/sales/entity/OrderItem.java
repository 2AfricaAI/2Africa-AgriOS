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
@TableName("order_item")
public class OrderItem {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;
    private Long skuId;
    private BigDecimal qty;
    private BigDecimal unitPrice;

    /** qty * unitPrice */
    private BigDecimal amount;

    /** 累计已发货数量 */
    private BigDecimal qtyShipped;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
