package ai.toafrica.agrios.qc.vo;

import lombok.Data;
import java.util.List;

@Data
public class QcInspectionDetailVO {
    private QcInspectionVO header;
    private List<QcInspectionItemVO> items;
}
