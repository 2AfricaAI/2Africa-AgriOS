package ai.toafrica.agrios.warehouse.vo;

import lombok.Data;
import java.util.List;

@Data
public class InboundDetailVO {
    private InboundVO header;
    private List<InboundItemVO> items;
}
