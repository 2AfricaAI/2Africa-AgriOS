package ai.toafrica.agrios.production.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Dashboard 聚合查询 - 不挂任何 entity, 全部返回原始 Map/Long/BigDecimal
 * 简单粗暴, 适合一次性首页用
 */
@Mapper
public interface DashboardMapper {

    // ===== KPI numbers =====

    @Select("""
            SELECT COUNT(*) FROM planting_plan
             WHERE deleted_at IS NULL
               AND status IN ('planned', 'in_progress')
            """)
    long countActivePlans();

    @Select("SELECT COUNT(*) FROM activity WHERE audit_status = 'pending'")
    long countPendingActivities();

    @Select("""
            SELECT COALESCE(SUM(qty_kg), 0) FROM harvest_record
             WHERE harvest_date = #{date}
            """)
    BigDecimal sumHarvestByDate(@Param("date") LocalDate date);

    @Select("""
            SELECT COUNT(*) FROM batch
             WHERE deleted_at IS NULL
               AND status = 'pending'
            """)
    long countPendingBatches();

    // ===== Time series =====

    @Select("""
            SELECT harvest_date AS d, COALESCE(SUM(qty_kg), 0) AS qty
              FROM harvest_record
             WHERE harvest_date >= #{from}
               AND harvest_date <= #{to}
             GROUP BY harvest_date
             ORDER BY harvest_date
            """)
    List<Map<String, Object>> harvestByDateRange(@Param("from") LocalDate from,
                                                  @Param("to") LocalDate to);

    @Select("""
            SELECT c.id AS crop_id, c.name AS crop_name,
                   COALESCE(SUM(h.qty_kg), 0) AS qty
              FROM harvest_record h
              LEFT JOIN crop c ON h.crop_id = c.id
             GROUP BY c.id, c.name
             ORDER BY qty DESC
             LIMIT 10
            """)
    List<Map<String, Object>> harvestByCrop();

    // ===== Top lists =====

    @Select("""
            SELECT a.id, a.activity_type, a.occur_date,
                   pp.code AS plan_code, p.name AS plot_name,
                   op.nickname AS operator_name
              FROM activity a
              LEFT JOIN planting_plan pp ON a.plan_id = pp.id
              LEFT JOIN plot p           ON a.plot_id = p.id
              LEFT JOIN sys_user op      ON a.operator_id = op.id
             WHERE a.audit_status = 'pending'
             ORDER BY a.occur_date DESC, a.id DESC
             LIMIT 5
            """)
    List<Map<String, Object>> topPendingActivities();

    @Select("""
            SELECT h.id, h.code, h.harvest_date, h.qty_kg,
                   c.name AS crop_name, p.name AS plot_name,
                   b.code AS batch_code
              FROM harvest_record h
              LEFT JOIN crop  c ON h.crop_id = c.id
              LEFT JOIN plot  p ON h.plot_id = p.id
              LEFT JOIN batch b ON h.batch_id = b.id
             ORDER BY h.harvest_date DESC, h.id DESC
             LIMIT 5
            """)
    List<Map<String, Object>> topRecentHarvests();
}
