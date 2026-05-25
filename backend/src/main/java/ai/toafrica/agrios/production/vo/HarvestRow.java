package ai.toafrica.agrios.production.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class HarvestRow {
    private Long id;
    private String code;
    private String clientUuid;

    private Long plotId;
    private String plotCode;
    private String plotName;

    private Long planId;
    private String planCode;

    private Long cropId;
    private String cropName;

    private Long varietyId;
    private String varietyName;

    private Long batchId;
    private String batchCode;

    private LocalDate harvestDate;
    private BigDecimal qtyKg;

    private Long operatorId;
    private String operatorName;

    /** photos JSON 原值, 由 Service 解析后 enrich 成 FileVO[] */
    private String photosJson;

    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
