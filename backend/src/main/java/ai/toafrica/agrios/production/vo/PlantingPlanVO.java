package ai.toafrica.agrios.production.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 种植计划列表 VO - 包含 JOIN 出来的地块/作物/品种名称
 */
@Data
@Schema(description = "Planting plan display object")
public class PlantingPlanVO {
    private Long id;
    private String code;

    private Long plotId;
    private String plotCode;
    private String plotName;

    private Long cropId;
    private String cropCode;
    private String cropName;

    /** 可为空(没指定具体品种) */
    private Long varietyId;
    private String varietyCode;
    private String varietyName;

    private BigDecimal areaMu;
    private LocalDate planStartDate;
    private LocalDate planHarvestDate;
    private LocalDate actualStartDate;
    private LocalDate actualFinishDate;
    private BigDecimal targetYieldKg;

    private String status;
    private String remark;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
