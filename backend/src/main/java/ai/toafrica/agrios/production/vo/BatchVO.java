package ai.toafrica.agrios.production.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "批次展示对象 - 全链路追溯主键")
public class BatchVO {
    private Long id;
    private String code;

    private Long parentBatchId;
    private String parentBatchCode;

    private Long plotId;
    private String plotCode;
    private String plotName;

    private Long planId;
    private String planCode;

    private Long cropId;
    private String cropName;

    private Long varietyId;
    private String varietyName;

    private Long harvestRecordId;
    private String harvestRecordCode;

    private LocalDate harvestDate;
    private BigDecimal qtyKg;
    private BigDecimal qtyRemainKg;

    /** pending / processing / packed / sold_out / lost */
    private String status;
    private String remark;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
