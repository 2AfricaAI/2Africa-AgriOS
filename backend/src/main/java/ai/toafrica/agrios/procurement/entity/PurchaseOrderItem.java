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

    /** 软外键 -> input_item.id (Sprint 22.1: PO 打通主数据) */
    private Long inputItemId;

    /** labor / water / electricity / fertilizer / seed / pesticide / equipment / service / other */
    private String inputType;
    /** 下单快照名称 (即使后续主数据改名也不影响历史 PO) */
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
