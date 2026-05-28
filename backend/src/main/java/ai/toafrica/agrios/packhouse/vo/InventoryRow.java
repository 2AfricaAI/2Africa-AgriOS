package ai.toafrica.agrios.packhouse.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class InventoryRow {
    private Long id;
    private Long skuId;
    private String skuCode;
    private String skuName;
    private Long batchId;
    private String batchCode;
    private String grade;
    private Long locationId;
    private String locationCode;
    private String locationName;
    private BigDecimal qtyAvail;
    private BigDecimal qtyLocked;
    private BigDecimal qtyInTransit;
    private String unit;
    private LocalDate prodDate;
    /** Best-before date - drives FEFO. */
    private LocalDate expiryDate;
    /** DATEDIFF(expiry_date, CURRENT_DATE). Negative = already expired. */
    private Integer daysToExpiry;
    private String status;
    private LocalDateTime lastOpAt;
    private LocalDateTime updatedAt;
    private String cropName;
    private String varietyName;
}
