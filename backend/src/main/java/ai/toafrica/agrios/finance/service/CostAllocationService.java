package ai.toafrica.agrios.finance.service;

import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.finance.mapper.CostMapper;
import ai.toafrica.agrios.finance.vo.PlanPnLVO;
import ai.toafrica.agrios.finance.vo.PnLListRow;
import ai.toafrica.agrios.production.entity.PlantingPlan;
import ai.toafrica.agrios.production.mapper.PlantingPlanMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 成本归集与 P&L 计算 - Sprint 12 核心.
 *
 * 单一职责: 把已有的 activity / activity_input / revenue 三张表的散点数据,
 * 聚合成 plan / batch 级的 P&L 报表 (V2.0 Phase 2 Sprint 14 报表页消费此 service).
 *
 * 设计原则:
 *   - 无副作用, 不写库
 *   - 所有归零 default 都在 SQL COALESCE, 不在 Java 里 if-null
 *   - 货币暂取最新 activity / revenue 的 currency, 多币种问题留到 Phase 3 处理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CostAllocationService {

    private final CostMapper costMapper;
    private final PlantingPlanMapper planMapper;

    /**
     * 计划级 P&L: 全部成本 + 全部收入 + 毛利.
     */
    public PlanPnLVO getPlanPnL(Long planId) {
        PlantingPlan plan = planMapper.selectById(planId);
        if (plan == null) throw new BusinessException(R.NOT_FOUND, "Plan not found");

        Map<String, Object> cost = costMapper.sumPlanCost(planId);
        Map<String, Object> rev  = costMapper.sumPlanRevenue(planId);

        PlanPnLVO vo = new PlanPnLVO();
        vo.setPlanId(planId);
        vo.setPlanCode(plan.getCode());

        // 成本 bucket
        vo.setLaborCost(toBig(cost.get("labor_cost")));
        vo.setWaterCost(toBig(cost.get("water_cost")));
        vo.setElectricityCost(toBig(cost.get("electricity_cost")));
        vo.setFertilizerCost(toBig(cost.get("fertilizer_cost")));
        vo.setOtherCost(toBig(cost.get("other_cost")));
        vo.setInputCost(toBig(cost.get("input_cost")));
        vo.setTotalCost(vo.getLaborCost()
                .add(vo.getWaterCost())
                .add(vo.getElectricityCost())
                .add(vo.getFertilizerCost())
                .add(vo.getOtherCost())
                .add(vo.getInputCost()));
        vo.setActivityCount(toInt(cost.get("activity_count")));

        // 收入
        vo.setTotalRevenue(toBig(rev.get("total_revenue")));

        // 毛利
        vo.setGrossProfit(vo.getTotalRevenue().subtract(vo.getTotalCost()));
        if (vo.getTotalRevenue().signum() > 0) {
            vo.setGrossMarginPct(vo.getGrossProfit()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(vo.getTotalRevenue(), 2, RoundingMode.HALF_UP));
        } else {
            vo.setGrossMarginPct(null);
        }

        // 货币 (优先取 revenue 货币, 没有就取 activity 货币)
        String revCurrency = (String) rev.get("currency");
        String costCurrency = (String) cost.get("currency");
        vo.setCurrency(revCurrency != null ? revCurrency :
                       (costCurrency != null ? costCurrency : "KES"));

        return vo;
    }

    /**
     * 批次级 P&L: 成本按 batch.qty_kg / plan 总产量比例分摊.
     */
    public PlanPnLVO getBatchPnL(Long batchId) {
        Map<String, Object> b = costMapper.getBatchPlanAndQty(batchId);
        if (b == null || b.isEmpty()) {
            throw new BusinessException(R.NOT_FOUND, "Batch not found");
        }
        Long planId = toLong(b.get("plan_id"));
        BigDecimal batchQty = toBig(b.get("qty_kg"));

        // 拿计划级成本
        PlanPnLVO planPnL = getPlanPnL(planId);

        // 按 batch / plan 总产量比例分摊
        BigDecimal planTotalQty = costMapper.sumPlanBatchQty(planId);
        BigDecimal ratio = planTotalQty.signum() > 0
                ? batchQty.divide(planTotalQty, 6, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        PlanPnLVO vo = new PlanPnLVO();
        vo.setPlanId(planId);
        vo.setPlanCode("BATCH:" + batchId);
        vo.setLaborCost(scale(planPnL.getLaborCost().multiply(ratio)));
        vo.setWaterCost(scale(planPnL.getWaterCost().multiply(ratio)));
        vo.setElectricityCost(scale(planPnL.getElectricityCost().multiply(ratio)));
        vo.setFertilizerCost(scale(planPnL.getFertilizerCost().multiply(ratio)));
        vo.setOtherCost(scale(planPnL.getOtherCost().multiply(ratio)));
        vo.setInputCost(scale(planPnL.getInputCost().multiply(ratio)));
        vo.setTotalCost(scale(planPnL.getTotalCost().multiply(ratio)));
        vo.setActivityCount(planPnL.getActivityCount());

        // batch 实际收入 (不是按比例分, 是真实金额)
        Map<String, Object> rev = costMapper.sumBatchRevenue(batchId);
        vo.setTotalRevenue(toBig(rev.get("total_revenue")));

        vo.setGrossProfit(vo.getTotalRevenue().subtract(vo.getTotalCost()));
        if (vo.getTotalRevenue().signum() > 0) {
            vo.setGrossMarginPct(vo.getGrossProfit()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(vo.getTotalRevenue(), 2, RoundingMode.HALF_UP));
        }

        String revCurrency = (String) rev.get("currency");
        vo.setCurrency(revCurrency != null ? revCurrency : planPnL.getCurrency());
        return vo;
    }

    // ===== 类型转换工具 =====
    private static BigDecimal toBig(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal b) return b;
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(o.toString());
    }
    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        return Long.parseLong(o.toString());
    }
    private static Integer toInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.intValue();
        return Integer.parseInt(o.toString());
    }
    private static BigDecimal scale(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    // ============================================================
    // Sprint 13 - Plot P&L
    // ============================================================
    public PlanPnLVO getPlotPnL(Long plotId, LocalDate from, LocalDate to) {
        Map<String, Object> cost = costMapper.sumPlotCost(plotId, from, to);
        Map<String, Object> rev  = costMapper.sumPlotRevenue(plotId);

        PlanPnLVO vo = new PlanPnLVO();
        vo.setPlanId(plotId);
        vo.setPlanCode("PLOT:" + plotId);
        vo.setLaborCost(toBig(cost.get("labor_cost")));
        vo.setWaterCost(toBig(cost.get("water_cost")));
        vo.setElectricityCost(toBig(cost.get("electricity_cost")));
        vo.setFertilizerCost(toBig(cost.get("fertilizer_cost")));
        vo.setOtherCost(toBig(cost.get("other_cost")));
        vo.setInputCost(toBig(cost.get("input_cost")));
        vo.setTotalCost(vo.getLaborCost()
                .add(vo.getWaterCost()).add(vo.getElectricityCost())
                .add(vo.getFertilizerCost()).add(vo.getOtherCost()).add(vo.getInputCost()));
        vo.setActivityCount(toInt(cost.get("activity_count")));
        vo.setTotalRevenue(toBig(rev.get("total_revenue")));
        vo.setGrossProfit(vo.getTotalRevenue().subtract(vo.getTotalCost()));
        if (vo.getTotalRevenue().signum() > 0) {
            vo.setGrossMarginPct(vo.getGrossProfit().multiply(BigDecimal.valueOf(100))
                    .divide(vo.getTotalRevenue(), 2, RoundingMode.HALF_UP));
        }
        vo.setCurrency(firstNonNull((String) rev.get("currency"),
                                    (String) cost.get("currency"), "KES"));
        return vo;
    }

    // ============================================================
    // Sprint 13 - SKU P&L
    //   cost = Σ (plan_cost_per_kg × revenue_row_qty_kg)
    //   plan_cost_per_kg = plan_total_cost / plan_total_batch_qty_kg
    // ============================================================
    public PlanPnLVO getSkuPnL(Long skuId) {
        List<Map<String, Object>> rows = costMapper.findSkuRevenueRows(skuId);
        Map<String, Object> revSum = costMapper.sumSkuRevenue(skuId);

        BigDecimal totalRevenue = toBig(revSum.get("total_revenue"));
        BigDecimal totalCost = BigDecimal.ZERO;

        // 缓存每个 plan 的 cost_per_kg 避免重复查
        Map<Long, BigDecimal> planCostPerKg = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long planId = toLong(row.get("plan_id"));
            BigDecimal qtyUnits = toBig(row.get("qty"));
            BigDecimal unitNetKg = toBig(row.get("unit_net_kg"));
            BigDecimal kgSold = qtyUnits.multiply(unitNetKg);

            BigDecimal cpk = planCostPerKg.computeIfAbsent(planId, pid -> {
                PlanPnLVO planP = getPlanPnL(pid);
                BigDecimal planTotalKg = costMapper.sumPlanBatchQty(pid);
                if (planTotalKg.signum() <= 0) return BigDecimal.ZERO;
                return planP.getTotalCost().divide(planTotalKg, 6, RoundingMode.HALF_UP);
            });

            totalCost = totalCost.add(cpk.multiply(kgSold));
        }
        totalCost = scale(totalCost);

        PlanPnLVO vo = new PlanPnLVO();
        vo.setPlanId(skuId);
        vo.setPlanCode("SKU:" + skuId);
        // SKU 维度下 cost bucket 没法细分(因为是聚合的), 全放在 inputCost+其他 buckets 留空
        vo.setLaborCost(BigDecimal.ZERO);
        vo.setWaterCost(BigDecimal.ZERO);
        vo.setElectricityCost(BigDecimal.ZERO);
        vo.setFertilizerCost(BigDecimal.ZERO);
        vo.setOtherCost(BigDecimal.ZERO);
        vo.setInputCost(totalCost); // 借这个字段显示总成本
        vo.setTotalCost(totalCost);
        vo.setActivityCount(rows.size());
        vo.setTotalRevenue(totalRevenue);
        vo.setGrossProfit(totalRevenue.subtract(totalCost));
        if (totalRevenue.signum() > 0) {
            vo.setGrossMarginPct(vo.getGrossProfit().multiply(BigDecimal.valueOf(100))
                    .divide(totalRevenue, 2, RoundingMode.HALF_UP));
        }
        vo.setCurrency(firstNonNull((String) revSum.get("currency"), "KES"));
        return vo;
    }

    // ============================================================
    // Sprint 13 - 列表视图 (报表页用)
    // ============================================================
    public List<PnLListRow> listPlanPnL() {
        return mapRows(costMapper.listPlanPnL(), "plan");
    }
    public List<PnLListRow> listPlotPnL() {
        return mapRows(costMapper.listPlotPnL(), "plot");
    }
    public List<PnLListRow> listSkuPnL() {
        List<Map<String, Object>> raw = costMapper.listSkuPnL();
        List<PnLListRow> out = new ArrayList<>();
        for (Map<String, Object> m : raw) {
            PnLListRow r = baseRow(m, "sku");
            // SKU cost 用 getSkuPnL 算 (精确,虽然性能略差)
            PlanPnLVO pnl = getSkuPnL(r.getRefId());
            r.setTotalCost(pnl.getTotalCost());
            r.setGrossProfit(pnl.getGrossProfit());
            r.setGrossMarginPct(pnl.getGrossMarginPct());
            out.add(r);
        }
        return out;
    }

    private List<PnLListRow> mapRows(List<Map<String, Object>> raw, String refType) {
        List<PnLListRow> out = new ArrayList<>(raw.size());
        for (Map<String, Object> m : raw) {
            out.add(baseRow(m, refType));
        }
        return out;
    }
    private PnLListRow baseRow(Map<String, Object> m, String refType) {
        PnLListRow r = new PnLListRow();
        r.setRefId(toLong(m.get("ref_id")));
        r.setRefType(refType);
        r.setRefCode((String) m.get("ref_code"));
        r.setRefName((String) m.get("ref_name"));
        r.setDimInfo((String) m.get("dim_info"));
        BigDecimal cost = toBig(m.get("total_cost"));
        BigDecimal rev  = toBig(m.get("total_revenue"));
        r.setTotalCost(cost);
        r.setTotalRevenue(rev);
        r.setGrossProfit(rev.subtract(cost));
        if (rev.signum() > 0) {
            r.setGrossMarginPct(r.getGrossProfit().multiply(BigDecimal.valueOf(100))
                    .divide(rev, 2, RoundingMode.HALF_UP));
        }
        r.setCurrency(firstNonNull((String) m.get("currency"), "KES"));
        return r;
    }

    private static String firstNonNull(String... vals) {
        for (String v : vals) if (v != null) return v;
        return null;
    }

    // ============================================================
    // Sprint 14 - Customer / Channel P&L
    // ============================================================
    public List<PnLListRow> listCustomerPnL() {
        return mapRows(costMapper.listCustomerPnL(), "customer");
    }

    public List<PnLListRow> listChannelPnL() {
        List<Map<String, Object>> raw = costMapper.listChannelPnL();
        List<PnLListRow> out = new ArrayList<>();
        for (Map<String, Object> m : raw) {
            PnLListRow r = new PnLListRow();
            r.setRefType("channel");
            r.setRefCode((String) m.get("ref_code"));
            r.setRefName((String) m.get("ref_name"));
            // dimInfo 在 channel 模式下是该渠道客户数
            Object cnt = m.get("dim_info");
            r.setDimInfo(cnt == null ? "0" : cnt.toString() + " customers");
            BigDecimal cost = toBig(m.get("total_cost"));
            BigDecimal rev = toBig(m.get("total_revenue"));
            r.setTotalCost(cost);
            r.setTotalRevenue(rev);
            r.setGrossProfit(rev.subtract(cost));
            if (rev.signum() > 0) {
                r.setGrossMarginPct(r.getGrossProfit().multiply(BigDecimal.valueOf(100))
                        .divide(rev, 2, RoundingMode.HALF_UP));
            }
            r.setCurrency(firstNonNull((String) m.get("currency"), "KES"));
            out.add(r);
        }
        return out;
    }
}
