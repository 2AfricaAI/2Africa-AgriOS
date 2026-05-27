package ai.toafrica.agrios.master.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class InputItemVO {
    private Long id;
    private String code;
    private String name;
    private String nameEn;
    private String inputType;
    private String categoryL2;           // Sprint 22.1.5
    private String spec;
    private BigDecimal packQty;          // Sprint 22.1.5
    private String packUnitLabel;        // Sprint 22.1.5
    private String unit;
    private String activeIngredient;
    private String registrationNo;
    private Integer phiDays;
    private Long defaultSupplierId;
    private String defaultSupplierName;  // populated by service via supplier mapper
    private Long defaultWarehouseId;     // Sprint 22.1.5
    private String defaultWarehouseName; // populated by service via warehouse mapper
    private BigDecimal minStockQty;      // Sprint 22.1.5
    private String status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
