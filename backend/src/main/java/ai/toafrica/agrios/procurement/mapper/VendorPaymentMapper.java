package ai.toafrica.agrios.procurement.mapper;

import ai.toafrica.agrios.procurement.entity.VendorPayment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface VendorPaymentMapper extends BaseMapper<VendorPayment> {

    @Select("""
            SELECT COALESCE(SUM(amount_kes), 0)
              FROM vendor_payment
             WHERE po_id = #{poId}
               AND status IN ('cleared', 'partial')
            """)
    BigDecimal sumByPo(@Param("poId") Long poId);

    /**
     * AP 应付账款台账 (按供应商聚合 + 账龄分桶, 镜像 AR).
     * 返回: supplier_id, supplier_code, supplier_name, supplier_type,
     *       total_billed, total_paid, ap_outstanding,
     *       aging_0_7, aging_8_14, aging_15_30, aging_30_plus
     */
    @Select("""
            SELECT
              s.id AS supplier_id, s.code AS supplier_code,
              s.name AS supplier_name, s.type AS supplier_type,
              COALESCE(SUM(o.total_amount), 0) AS total_billed,
              COALESCE(SUM(p.paid), 0) AS total_paid,
              COALESCE(SUM(o.total_amount), 0) - COALESCE(SUM(p.paid), 0) AS ap_outstanding,
              COALESCE(SUM(CASE WHEN DATEDIFF(CURDATE(), o.order_date) BETWEEN 0 AND 7
                                  THEN o.total_amount - COALESCE(p.paid, 0)
                                  ELSE 0 END), 0) AS aging_0_7,
              COALESCE(SUM(CASE WHEN DATEDIFF(CURDATE(), o.order_date) BETWEEN 8 AND 14
                                  THEN o.total_amount - COALESCE(p.paid, 0)
                                  ELSE 0 END), 0) AS aging_8_14,
              COALESCE(SUM(CASE WHEN DATEDIFF(CURDATE(), o.order_date) BETWEEN 15 AND 30
                                  THEN o.total_amount - COALESCE(p.paid, 0)
                                  ELSE 0 END), 0) AS aging_15_30,
              COALESCE(SUM(CASE WHEN DATEDIFF(CURDATE(), o.order_date) > 30
                                  THEN o.total_amount - COALESCE(p.paid, 0)
                                  ELSE 0 END), 0) AS aging_30_plus
              FROM supplier s
              JOIN purchase_order o ON o.supplier_id = s.id AND o.deleted_at IS NULL
              LEFT JOIN (
                SELECT po_id, SUM(amount_kes) AS paid
                  FROM vendor_payment
                 WHERE status IN ('cleared', 'partial')
                 GROUP BY po_id
              ) p ON p.po_id = o.id
              WHERE s.deleted_at IS NULL
                AND o.status NOT IN ('cancelled')
              GROUP BY s.id, s.code, s.name, s.type
              HAVING ap_outstanding > 0
              ORDER BY ap_outstanding DESC
            """)
    List<Map<String, Object>> apAgingBySupplier();
}
