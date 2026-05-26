package ai.toafrica.agrios.packhouse.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PackingRow {
    private Long id;
    private String code;
    private Long batchId;
    private String batchCode;
    private String grade;
    private Long specId;
    private String specCode;
    private String specName;
    private Long skuId;
    private String skuCode;
    private String skuName;
    private Integer qtyUnits;
    private BigDecimal netWeightKg;
    private Long locationId;
    private String locationCode;
    private String locationName;
    private LocalDateTime packedAt;
    private Long operatorId;
    private String operatorName;
    private String remark;
    private LocalDateTime createdAt;
    private String cropName;  // 通过 sku 取的作物
}
