package ai.toafrica.agrios.production.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Activity 的"半成品行"
 *
 * Mapper 的 JOIN 查询返回这个,Service 层再把 photosJson 解析 + enrich 成 List&lt;FileVO&gt;
 * 才得到最终的 ActivityVO。
 *
 * 这里所有字段都是 camelCase,跟 SQL 的 snake_case 列名通过
 * map-underscore-to-camel-case (application.yml 已开启) 自动映射。
 */
@Data
public class ActivityRow {
    private Long id;
    private String clientUuid;

    private Long plotId;
    private String plotCode;
    private String plotName;

    private Long planId;
    private String planCode;

    private String activityType;
    private LocalDate occurDate;

    private Long operatorId;
    private String operatorName;

    /** photos 列的原 JSON 串 (例如 "[1,2,3]"),Service 解析后转 List&lt;Long&gt; 再 enrich */
    private String photosJson;

    private String locationGps;
    private String remark;

    /** Sprint 11 - 成本字段 (V2.0 Phase 2 P&L) */
    private BigDecimal laborCost;
    private BigDecimal waterCost;
    private BigDecimal electricityCost;
    private BigDecimal fertilizerCost;
    private BigDecimal otherCost;
    private String costCurrency;

    /** Sprint 17.7 - PO 行 FK */
    private Long laborPoItemId;
    private Long waterPoItemId;
    private Long electricityPoItemId;
    private Long fertilizerPoItemId;
    private Long otherPoItemId;

    private String auditStatus;
    private Long auditorId;
    private String auditorName;
    private LocalDateTime auditedAt;
    private String auditRemark;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
