package ai.toafrica.agrios.sales.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "Fulfillment item with SKU + batch + inventory details")
public class FulfillmentItemVO {
    private Long id;
    private Long fulfillmentId;
    private Long orderItemId;

    private Long inventoryId;
    private String locationCode;
    private String locationName;

    private Long batchId;
    private String batchCode;
    private LocalDate batchHarvestDate;

    private Long skuId;
    private String skuCode;
    private String skuName;
    private String grade;

    private String cropName;
    private String varietyName;
    private String specName;

    private BigDecimal qty;
}
