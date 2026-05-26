package ai.toafrica.agrios.procurement.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("purchase_order_item")
public class PurchaseOrderItem {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long poId;

    /** labor / water / electricity / fertilizer / seed / pesticide / equipment / service / other */
    private String inputType;
    private String description;

    private BigDecimal quantity;
    /** bag / kg / L / hour / person-day / lump-sum */
    private String unit;
    private BigDecimal unitPrice;
    /** = quantity × unitPrice */
    private BigDecimal amount;

    /** 已收数量 (支持部分到货) */
    private BigDecimal receivedQty;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
