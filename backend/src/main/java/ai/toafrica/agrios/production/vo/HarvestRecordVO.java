package ai.toafrica.agrios.production.vo;

import ai.toafrica.agrios.system.vo.FileVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "Harvest record display object")
public class HarvestRecordVO {
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

    /** 自动生成的 batch */
    private Long batchId;
    private String batchCode;

    private LocalDate harvestDate;
    private BigDecimal qtyKg;

    private Long operatorId;
    private String operatorName;

    private List<FileVO> photos;
    private String remark;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
