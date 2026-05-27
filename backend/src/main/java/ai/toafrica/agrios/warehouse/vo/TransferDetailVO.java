package ai.toafrica.agrios.warehouse.vo;

import lombok.Data;
import java.util.List;

@Data
public class TransferDetailVO {
    private TransferVO header;
    private List<TransferItemVO> items;
}
