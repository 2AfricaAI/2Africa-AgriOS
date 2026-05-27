package ai.toafrica.agrios.master.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 投入品库存 VO (Sprint 22.2)
 * 列表展示用, 附带物料和仓库的名称.
 */
@Data
public class InputStockVO {
    private Long id;
    private Long inputItemId;
    private String inputItemCode;
    private String inputItemName;
    private String inputType;
    private String unit;

    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;

    private BigDecimal qtyOnHand;
    private BigDecimal qtyReserved;
    /** available = qtyOnHand - qtyReserved (computed in SQL) */
    private BigDecimal qtyAvailable;

    private LocalDateTime lastStockAt;
}
