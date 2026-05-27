package ai.toafrica.agrios.master.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 出入库流水 VO (Sprint 22.3)
 */
@Data
public class InputStockLogVO {
    private Long id;

    private Long inputItemId;
    private String inputItemCode;
    private String inputItemName;
    private String unit;

    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;

    private String direction;
    private BigDecimal qty;
    private String reasonType;
    private String referenceType;
    private Long referenceId;
    private BigDecimal qtyAfter;

    private Long operatorId;
    private String operatorName;
    private String remark;
    private LocalDateTime createdAt;
}
