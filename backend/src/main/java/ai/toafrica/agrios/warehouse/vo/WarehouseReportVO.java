package ai.toafrica.agrios.warehouse.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Warehouse operations report aggregation VO (Sprint 22.9b).
 * One endpoint returns all dashboard cards + breakdowns.
 */
@Data
public class WarehouseReportVO {

    /** Period summary: doc counts by type × status */
    private Map<String, Map<String, Integer>> docCounts;
    // e.g. {"inbound":{"draft":3,"confirmed":12,"cancelled":1},"outbound":{...},...}

    /** Top N items by total inbound qty in period */
    private List<ItemMovement> topInbound;

    /** Top N items by total outbound qty in period */
    private List<ItemMovement> topOutbound;

    /** Items currently below min_stock_qty */
    private List<LowStockItem> lowStock;

    /** Stock value distribution by warehouse purpose */
    private List<WarehouseStockSummary> stockByWarehouse;

    @Data
    public static class ItemMovement {
        private Long inputItemId;
        private String inputItemCode;
        private String inputItemName;
        private String unit;
        private BigDecimal totalQty;
        private int txnCount;
    }

    @Data
    public static class LowStockItem {
        private Long inputItemId;
        private String inputItemCode;
        private String inputItemName;
        private String unit;
        private Long warehouseId;
        private String warehouseName;
        private BigDecimal qtyOnHand;
        private BigDecimal qtyReserved;
        private BigDecimal qtyAvailable;
        private BigDecimal minStockQty;
        private BigDecimal shortageQty;
    }

    @Data
    public static class WarehouseStockSummary {
        private Long warehouseId;
        private String warehouseCode;
        private String warehouseName;
        private String purpose;
        private int itemCount;
        private BigDecimal totalQty;
    }
}
