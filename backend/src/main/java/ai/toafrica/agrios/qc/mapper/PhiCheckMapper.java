package ai.toafrica.agrios.qc.mapper;

import ai.toafrica.agrios.qc.vo.PhiCheckVO.BlockingSpray;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface PhiCheckMapper {

    /**
     * 查给定 plan 下所有 spray 活动用的农药, 按 (activity_id, input_item_id) 一行返回.
     * 只看 phi_days > 0 的 input_item.
     *
     * 后台代码再筛 "未过安全期" 的行 (occur_date + phi_days > today).
     */
    @Select("""
            SELECT a.id          AS activity_id,
              a.occur_date       AS spray_date,
              ai.input_id        AS input_item_id,
              ii.code            AS input_item_code,
              COALESCE(ii.name_en, ii.name) AS input_item_name,
              ii.active_ingredient AS active_ingredient,
              ii.phi_days        AS phi_days,
              DATE_ADD(a.occur_date, INTERVAL ii.phi_days DAY) AS safe_after,
              ai.qty             AS qty,
              ai.unit            AS unit
            FROM activity a
            JOIN activity_input ai ON ai.activity_id = a.id
            JOIN input_item ii      ON ii.id = ai.input_id
            WHERE a.plan_id = #{planId}
              AND a.activity_type = 'spray'
              AND ii.phi_days IS NOT NULL
              AND ii.phi_days > 0
              AND a.occur_date >= #{since}
            ORDER BY a.occur_date DESC
            """)
    List<BlockingSpray> findRecentSprays(@Param("planId") Long planId,
                                          @Param("since") LocalDate since);

    /**
     * 同上, 但按 plotId 查 (一个 plot 可能有多个 plan, 农药残留也会跨 plan)
     */
    @Select("""
            SELECT a.id          AS activity_id,
              a.occur_date       AS spray_date,
              ai.input_id        AS input_item_id,
              ii.code            AS input_item_code,
              COALESCE(ii.name_en, ii.name) AS input_item_name,
              ii.active_ingredient AS active_ingredient,
              ii.phi_days        AS phi_days,
              DATE_ADD(a.occur_date, INTERVAL ii.phi_days DAY) AS safe_after,
              ai.qty             AS qty,
              ai.unit            AS unit
            FROM activity a
            JOIN activity_input ai ON ai.activity_id = a.id
            JOIN input_item ii      ON ii.id = ai.input_id
            WHERE a.plot_id = #{plotId}
              AND a.activity_type = 'spray'
              AND ii.phi_days IS NOT NULL
              AND ii.phi_days > 0
              AND a.occur_date >= #{since}
            ORDER BY a.occur_date DESC
            """)
    List<BlockingSpray> findRecentSpraysByPlot(@Param("plotId") Long plotId,
                                                @Param("since") LocalDate since);
}
