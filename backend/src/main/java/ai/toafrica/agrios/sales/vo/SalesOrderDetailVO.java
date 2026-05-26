package ai.toafrica.agrios.sales.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Full sales order detail = header + items")
public class SalesOrderDetailVO {
    private SalesOrderVO order;
    private List<OrderItemVO> items;
}
