package ai.toafrica.agrios.procurement.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Full purchase order detail = header + items")
public class PurchaseOrderDetailVO {
    private PurchaseOrderVO order;
    private List<PurchaseOrderItemVO> items;
}
