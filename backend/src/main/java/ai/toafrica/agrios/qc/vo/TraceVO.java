package ai.toafrica.agrios.qc.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 批次完整追溯链 (Sprint 25 / Phase 5).
 *
 * 顺序:
 *   PO 入货 → 农事(喷药/施肥) → 采收 → 批次 → 包装 → 销售订单
 */
@Data
public class TraceVO {

    /** 批次本身 (起点) */
    private BatchNode batch;

    /** 上游 - 来源 */
    private HarvestNode harvest;
    private PlanNode plan;
    private PlotNode plot;
    private List<ActivityNode> activities;     // 该 plan 的所有 spray/fertilize 活动
    private List<InboundNode> inbounds;        // 用到的农药/肥料来自哪个 PO 入库

    /** 下游 - 流向 */
    private List<PackingNode> packings;        // 这个 batch 用到了哪些 packing
    private List<OrderNode> orders;            // 这些 packing 进了哪些 sales order

    @Data
    public static class BatchNode {
        private Long id;
        private String code;
        private String status;
        private BigDecimal qtyKg;
        private LocalDate createdDate;
        private String varietyName;
        private String cropName;
    }

    @Data
    public static class HarvestNode {
        private Long id;
        private String code;
        private LocalDate harvestDate;
        private BigDecimal qtyKg;
        private String operatorName;
        private String locationGps;
    }

    @Data
    public static class PlanNode {
        private Long id;
        private String code;
        private LocalDate plannedSowDate;
        private LocalDate plannedHarvestDate;
        private String status;
    }

    @Data
    public static class PlotNode {
        private Long id;
        private String code;
        private String name;
        private String regionName;
        private BigDecimal areaHa;
    }

    @Data
    public static class ActivityNode {
        private Long id;
        private String activityType;
        private LocalDate occurDate;
        private String operatorName;
        private List<InputUsed> inputs;
    }

    @Data
    public static class InputUsed {
        private Long inputItemId;
        private String inputItemCode;
        private String inputItemName;
        private String activeIngredient;
        private Integer phiDays;
        private BigDecimal qty;
        private String unit;
    }

    @Data
    public static class InboundNode {
        private Long id;
        private String code;
        private String sourceType;      // po_receive
        private Long sourceId;          // po id
        private String sourceCode;      // PO code
        private LocalDate confirmedAt;
        private List<InputReceived> items;
    }

    @Data
    public static class InputReceived {
        private Long inputItemId;
        private String inputItemCode;
        private String inputItemName;
        private BigDecimal qty;
    }

    @Data
    public static class PackingNode {
        private Long id;
        private String code;
        private LocalDate packDate;
        private String skuCode;
        private String skuName;
        private BigDecimal qty;
        private String unit;
    }

    @Data
    public static class OrderNode {
        private Long id;
        private String code;
        private LocalDate orderDate;
        private String customerName;
        private String status;
    }
}
