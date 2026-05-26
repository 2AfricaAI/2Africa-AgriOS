package ai.toafrica.agrios.production.vo;

import ai.toafrica.agrios.system.vo.FileVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "农事记录展示对象")
public class ActivityVO {
    private Long id;
    private String clientUuid;

    private Long plotId;
    private String plotCode;
    private String plotName;

    private Long planId;
    private String planCode;

    /** sow / fertilize / spray / weed / water / prune / other */
    private String activityType;
    private LocalDate occurDate;

    private Long operatorId;
    private String operatorName;

    /** 照片(已 enrich 成 FileVO,含 fresh 预签名 URL) */
    private List<FileVO> photos;

    private String locationGps;
    private String remark;

    /** Sprint 11 - 成本字段 (V2.0 Phase 2 P&L 种子数据) */
    private BigDecimal laborCost;
    private BigDecimal waterCost;
    private BigDecimal electricityCost;
    private BigDecimal fertilizerCost;
    private BigDecimal otherCost;
    private String costCurrency;

    private String auditStatus;
    private Long auditorId;
    private String auditorName;
    private LocalDateTime auditedAt;
    private String auditRemark;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
