package ai.toafrica.agrios.warehouse.vo;

import lombok.Data;
import java.util.List;

@Data
public class OutboundDetailVO {
    private OutboundVO header;
    private List<OutboundItemVO> items;
}
