package ai.toafrica.agrios.sales.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Full fulfillment detail = header + items")
public class FulfillmentDetailVO {
    private FulfillmentVO fulfillment;
    private List<FulfillmentItemVO> items;
}
