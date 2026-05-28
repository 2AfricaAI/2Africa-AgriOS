package ai.toafrica.agrios.qc.mapper;

import ai.toafrica.agrios.qc.entity.Recall;
import ai.toafrica.agrios.qc.vo.RecallVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface RecallMapper extends BaseMapper<Recall> {

    @Select("""
            SELECT
              r.id, r.code, r.triggered_at,
              r.source_complaint_id, sc.code AS source_complaint_code,
              r.batch_id, b.code AS batch_code,
              cr.name AS crop_name, v.name AS variety_name,
              r.scope, r.reason, r.status,
              r.affected_order_count, r.affected_customer_count, r.affected_qty,
              r.initiated_by_id, iu.nickname AS initiated_by_name,
              r.closed_at, r.closed_by_id, cu.nickname AS closed_by_name, r.closed_remark,
              r.created_at, r.updated_at
            FROM recall r
            LEFT JOIN complaint  sc ON r.source_complaint_id = sc.id
            LEFT JOIN batch      b  ON r.batch_id            = b.id
            LEFT JOIN crop       cr ON b.crop_id             = cr.id
            LEFT JOIN variety    v  ON b.variety_id          = v.id
            LEFT JOIN sys_user   iu ON r.initiated_by_id     = iu.id
            LEFT JOIN sys_user   cu ON r.closed_by_id        = cu.id
            ${ew.customSqlSegment}
            """)
    IPage<RecallVO> pageWithJoin(Page<RecallVO> page,
                                 @Param("ew") QueryWrapper<RecallVO> wrapper);

    @Select("""
            SELECT COUNT(*) FROM recall WHERE DATE(triggered_at) = #{day}
            """)
    int countByDate(@Param("day") LocalDate day);

    /**
     * Reverse lookup: which orders consumed inventory from this batch?
     * Returns one row per (order_id, customer_id) with summed qty.
     */
    @Select("""
            SELECT
              o.id AS order_id, o.code AS order_code,
              cu.id AS customer_id, cu.name AS customer_name,
              SUM(fi.qty) AS qty,
              MAX(f.shipped_at) AS delivered_at
            FROM fulfillment_item fi
            JOIN fulfillment   f  ON fi.fulfillment_id = f.id
            JOIN sales_order   o  ON f.order_id        = o.id
            JOIN customer      cu ON o.customer_id     = cu.id
            WHERE fi.batch_id = #{batchId}
              AND f.status IN ('ready', 'shipped', 'delivered')
            GROUP BY o.id, o.code, cu.id, cu.name
            ORDER BY o.id
            """)
    List<Map<String, Object>> findAffectedOrders(@Param("batchId") Long batchId);

    /**
     * Freeze inventory: status normal → frozen for all rows of this batch.
     * Returns the total qty_avail frozen (for the recall stats).
     */
    @Select("""
            SELECT COALESCE(SUM(qty_avail), 0) FROM inventory
            WHERE batch_id = #{batchId} AND status = 'normal'
            """)
    java.math.BigDecimal sumFrozenQty(@Param("batchId") Long batchId);
}
