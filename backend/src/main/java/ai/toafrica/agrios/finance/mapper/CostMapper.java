package ai.toafrica.agrios.finance.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 成本归集 - 直接走 SQL 聚合, 不绑 entity.
 *
 * 设计原则:
 *   - 每个查询返回 Map<String, Object> (列名 → 值), Service 层做类型转换
 *   - SUM(...) 返回 NULL 时 COALESCE 成 0, 避免前端处理 null
 *   - 跨表 JOIN 用子查询, 不依赖 MyBatis-Plus wrapper
 */
@Mapper
public interface CostMapper {

    /**
     * 计划级成本明细 (5 个 activity bucket + activity_input)
     * 返回字段: labor_cost, water_cost, electricity_cost, fertilizer_cost, other_cost,
     *          input_cost, activity_count, currency
     */
    @Select("""
            SELECT
              COALESCE(SUM(a.labor_cost), 0)        AS labor_cost,
              COALESCE(SUM(a.water_cost), 0)        AS water_cost,
              COALESCE(SUM(a.electricity_cost), 0)  AS electricity_cost,
              COALESCE(SUM(a.fertilizer_cost), 0)   AS fertilizer_cost,
              COALESCE(SUM(a.other_cost), 0)        AS other_cost,
              COALESCE((SELECT SUM(ai.cost)
                          FROM activity ax
                          JOIN activity_input ai ON ai.activity_id = ax.id
                         WHERE ax.plan_id = #{planId}), 0) AS input_cost,
              COUNT(a.id)                            AS activity_count,
              MAX(a.cost_currency)                   AS currency
              FROM activity a
              WHERE a.plan_id = #{planId}
            """)
    Map<String, Object> sumPlanCost(@Param("planId") Long planId);

    /**
     * 计划下所有 batches 的 revenue 汇总 (按 plan → batches → revenue 三层 join)
     */
    @Select("""
            SELECT COALESCE(SUM(r.net_amount), 0) AS total_revenue,
                   MAX(r.currency)                AS currency
              FROM revenue r
              JOIN batch  b ON r.batch_id = b.id
             WHERE b.plan_id = #{planId}
               AND r.status = 'recognized'
            """)
    Map<String, Object> sumPlanRevenue(@Param("planId") Long planId);

    /**
     * 单批次的 revenue (直接按 batch_id 过滤)
     */
    @Select("""
            SELECT COALESCE(SUM(r.net_amount), 0) AS total_revenue,
                   COALESCE(SUM(r.qty), 0)        AS total_qty,
                   MAX(r.currency)                AS currency
              FROM revenue r
             WHERE r.batch_id = #{batchId}
               AND r.status = 'recognized'
            """)
    Map<String, Object> sumBatchRevenue(@Param("batchId") Long batchId);

    /**
     * 取 batch 的 plan_id + qty_kg (用于按比例分摊计划成本)
     */
    @Select("SELECT plan_id, qty_kg FROM batch WHERE id = #{batchId} AND deleted_at IS NULL")
    Map<String, Object> getBatchPlanAndQty(@Param("batchId") Long batchId);

    /**
     * 计划下所有 batch 的总产量 (用于分摊基数)
     */
    @Select("""
            SELECT COALESCE(SUM(qty_kg), 0) AS total_qty
              FROM batch
             WHERE plan_id = #{planId}
               AND deleted_at IS NULL
            """)
    BigDecimal sumPlanBatchQty(@Param("planId") Long planId);

    // ============================================================
    // Sprint 13 - Plot 聚合
    // ============================================================

    /** 地块的成本汇总 (按 activity.plot_id 直接 SUM,日期范围可选 - 用 ? IS NULL 简化避免动态 SQL) */
    @Select("""
            SELECT
              COALESCE(SUM(a.labor_cost), 0)       AS labor_cost,
              COALESCE(SUM(a.water_cost), 0)       AS water_cost,
              COALESCE(SUM(a.electricity_cost), 0) AS electricity_cost,
              COALESCE(SUM(a.fertilizer_cost), 0)  AS fertilizer_cost,
              COALESCE(SUM(a.other_cost), 0)       AS other_cost,
              COALESCE((SELECT SUM(ai.cost)
                         FROM activity ax
                         JOIN activity_input ai ON ai.activity_id = ax.id
                        WHERE ax.plot_id = #{plotId}
                          AND (#{from} IS NULL OR ax.occur_date >= #{from})
                          AND (#{to}   IS NULL OR ax.occur_date <= #{to})
                       ), 0) AS input_cost,
              COUNT(a.id)                          AS activity_count,
              MAX(a.cost_currency)                 AS currency
              FROM activity a
              WHERE a.plot_id = #{plotId}
                AND (#{from} IS NULL OR a.occur_date >= #{from})
                AND (#{to}   IS NULL OR a.occur_date <= #{to})
            """)
    Map<String, Object> sumPlotCost(@Param("plotId") Long plotId,
                                    @Param("from") LocalDate from,
                                    @Param("to") LocalDate to);

    /** 地块的收入汇总 (batch.plot_id = plotId → revenue) */
    @Select("""
            SELECT COALESCE(SUM(r.net_amount), 0) AS total_revenue,
                   MAX(r.currency)                AS currency
              FROM revenue r
              JOIN batch  b ON r.batch_id = b.id
             WHERE b.plot_id = #{plotId}
               AND r.status = 'recognized'
            """)
    Map<String, Object> sumPlotRevenue(@Param("plotId") Long plotId);

    // ============================================================
    // Sprint 13 - SKU 聚合 (公式: cost = plan_cost_per_kg × sold_kg)
    // ============================================================

    /**
     * 拿某 SKU 涉及的所有 revenue 行 + 关联的 plan_id + spec.unit_net_kg.
     * Service 层用这些数据按"计划单位 kg 成本"乘法算 SKU 总成本.
     */
    @Select("""
            SELECT
              r.batch_id, b.plan_id,
              r.qty, ps.unit_net_kg,
              r.net_amount
              FROM revenue r
              JOIN batch          b  ON r.batch_id = b.id
              JOIN sku            s  ON r.sku_id   = s.id
              JOIN packaging_spec ps ON s.spec_id  = ps.id
              WHERE r.sku_id = #{skuId}
                AND r.status = 'recognized'
            """)
    List<Map<String, Object>> findSkuRevenueRows(@Param("skuId") Long skuId);

    /** SKU 收入汇总 */
    @Select("""
            SELECT
              COALESCE(SUM(r.net_amount), 0) AS total_revenue,
              COALESCE(SUM(r.qty), 0)        AS total_qty,
              MAX(r.currency)                AS currency
              FROM revenue r
             WHERE r.sku_id = #{skuId}
               AND r.status = 'recognized'
            """)
    Map<String, Object> sumSkuRevenue(@Param("skuId") Long skuId);

    // ============================================================
    // Sprint 13 - 列表视图 (报表页 3 tab 用)
    // ============================================================

    /** Plan P&L 列表 - 所有非删除计划的 cost / revenue 一次性出 */
    @Select("""
            SELECT
              pp.id AS ref_id, pp.code AS ref_code, c.name AS ref_name,
              v.name AS dim_info,
              -- cost
              COALESCE((SELECT SUM(a.labor_cost + a.water_cost + a.electricity_cost + a.fertilizer_cost + a.other_cost)
                         FROM activity a WHERE a.plan_id = pp.id), 0)
              + COALESCE((SELECT SUM(ai.cost)
                            FROM activity ax JOIN activity_input ai ON ai.activity_id = ax.id
                           WHERE ax.plan_id = pp.id), 0) AS total_cost,
              -- revenue
              COALESCE((SELECT SUM(r.net_amount)
                         FROM revenue r JOIN batch b ON r.batch_id = b.id
                        WHERE b.plan_id = pp.id AND r.status = 'recognized'), 0) AS total_revenue,
              -- currency (优先取 revenue 货币)
              COALESCE((SELECT MAX(r.currency)
                         FROM revenue r JOIN batch b ON r.batch_id = b.id
                        WHERE b.plan_id = pp.id), 'KES') AS currency
              FROM planting_plan pp
              LEFT JOIN crop    c ON pp.crop_id    = c.id
              LEFT JOIN variety v ON pp.variety_id = v.id
              WHERE pp.deleted_at IS NULL
              ORDER BY pp.id DESC
            """)
    List<Map<String, Object>> listPlanPnL();

    /** Plot P&L 列表 */
    @Select("""
            SELECT
              p.id AS ref_id, p.code AS ref_code, p.name AS ref_name,
              p.soil_type AS dim_info,
              COALESCE((SELECT SUM(a.labor_cost + a.water_cost + a.electricity_cost + a.fertilizer_cost + a.other_cost)
                         FROM activity a WHERE a.plot_id = p.id), 0)
              + COALESCE((SELECT SUM(ai.cost)
                            FROM activity ax JOIN activity_input ai ON ai.activity_id = ax.id
                           WHERE ax.plot_id = p.id), 0) AS total_cost,
              COALESCE((SELECT SUM(r.net_amount)
                         FROM revenue r JOIN batch b ON r.batch_id = b.id
                        WHERE b.plot_id = p.id AND r.status = 'recognized'), 0) AS total_revenue,
              COALESCE((SELECT MAX(r.currency)
                         FROM revenue r JOIN batch b ON r.batch_id = b.id
                        WHERE b.plot_id = p.id), 'KES') AS currency
              FROM plot p
              WHERE p.deleted_at IS NULL
              ORDER BY p.id DESC
            """)
    List<Map<String, Object>> listPlotPnL();

    /**
     * SKU P&L 列表 - 简化版.
     * 成本用 plan_cost_per_kg × kg_sold 的近似在 Service 层算,所以这里只返回 revenue.
     * 详细成本由 Service 调用 findSkuRevenueRows + plan cost 单独算.
     */
    @Select("""
            SELECT
              s.id AS ref_id, s.code AS ref_code, s.name AS ref_name,
              s.grade AS dim_info,
              COALESCE((SELECT SUM(r.net_amount)
                         FROM revenue r WHERE r.sku_id = s.id AND r.status = 'recognized'), 0) AS total_revenue,
              COALESCE((SELECT MAX(r.currency)
                         FROM revenue r WHERE r.sku_id = s.id), 'KES') AS currency
              FROM sku s
              WHERE EXISTS (SELECT 1 FROM revenue r WHERE r.sku_id = s.id)
              ORDER BY s.id DESC
            """)
    List<Map<String, Object>> listSkuPnL();

    // ============================================================
    // Sprint 14 - Customer / Channel P&L
    // ============================================================

    /**
     * 按客户聚合收入 (成本暂为 0 - 客户没有"种植成本", 物流/服务成本以后再细做).
     * 列: ref_id, ref_code, ref_name, dim_info(customer.type), total_revenue, currency
     */
    @Select("""
            SELECT
              c.id AS ref_id, c.code AS ref_code, c.name AS ref_name,
              c.type AS dim_info,
              COALESCE((SELECT SUM(r.net_amount)
                         FROM revenue r WHERE r.customer_id = c.id AND r.status = 'recognized'), 0) AS total_revenue,
              0 AS total_cost,
              COALESCE((SELECT MAX(r.currency)
                         FROM revenue r WHERE r.customer_id = c.id), 'KES') AS currency
              FROM customer c
              WHERE c.deleted_at IS NULL
                AND EXISTS (SELECT 1 FROM revenue r WHERE r.customer_id = c.id)
              ORDER BY total_revenue DESC
            """)
    List<Map<String, Object>> listCustomerPnL();

    /**
     * 按渠道(customer.type)聚合.
     * 渠道维度: supermarket / restaurant / ecommerce / wholesale / export / other
     */
    @Select("""
            SELECT
              c.type AS ref_code,
              c.type AS ref_name,
              COUNT(DISTINCT c.id) AS dim_info,    -- 该渠道下客户数
              COALESCE(SUM(r.net_amount), 0)       AS total_revenue,
              0                                     AS total_cost,
              MAX(r.currency)                       AS currency
              FROM customer c
              LEFT JOIN revenue r ON r.customer_id = c.id AND r.status = 'recognized'
              WHERE c.deleted_at IS NULL
                AND c.type IS NOT NULL
              GROUP BY c.type
              HAVING total_revenue > 0
              ORDER BY total_revenue DESC
            """)
    List<Map<String, Object>> listChannelPnL();
}
