package ai.toafrica.agrios.qc.vo;

import lombok.Data;

@Data
public class QcInspectionItemVO {
    private Long id;
    private Long inspectionId;
    private String checkPoint;
    private String expectedValue;
    private String actualValue;
    private String result;
    private String remark;
}
