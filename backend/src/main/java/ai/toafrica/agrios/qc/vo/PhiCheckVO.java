package ai.toafrica.agrios.qc.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * PHI (Pre-Harvest Interval) check result (Sprint 23 / Phase 5).
 *
 * 给定一个种植计划 (plan) 和拟采收日期,返回:
 *   - blocked: true 表示当下采收违反 PHI
 *   - earliestSafeDate: 最早可采收日 (max spray_date + max phi_days)
 *   - blockingActivities: 触发阻断的喷药明细
 */
@Data
public class PhiCheckVO {
    private Long planId;
    private LocalDate proposedDate;
    private LocalDate earliestSafeDate;
    private int daysRemaining;       // earliestSafeDate - today (>= 0)
    private boolean blocked;
    private List<BlockingSpray> blockingSprays;

    @Data
    public static class BlockingSpray {
        private Long activityId;
        private LocalDate sprayDate;
        private Long inputItemId;
        private String inputItemCode;
        private String inputItemName;
        private String activeIngredient;
        private Integer phiDays;
        private LocalDate safeAfter;
        private BigDecimal qty;
        private String unit;
    }
}
