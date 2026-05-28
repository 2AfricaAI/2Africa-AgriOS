package ai.toafrica.agrios.qc.vo;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class QcInspectionVO {
    private Long id;
    private String code;
    private String inspectionType;
    private String refType;
    private Long refId;
    private String refCode;
    private LocalDate inspectDate;
    private Long inspectorId;
    private String inspectorName;
    private String result;
    private String resultRemark;
    private List<Long> photoIds;
    private String remark;
    private LocalDateTime createdAt;
    private int itemCount;
}
