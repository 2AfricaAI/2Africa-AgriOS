package ai.toafrica.agrios.finance.mapper;

import ai.toafrica.agrios.finance.entity.Payment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {

    @Select("""
            SELECT COALESCE(SUM(amount_kes), 0)
              FROM payment
             WHERE order_id = #{orderId}
               AND status IN ('cleared', 'partial')
            """)
    java.math.BigDecimal sumByOrder(@Param("orderId") Long orderId);

    /**
     * AR 应收账款台账 (按客户聚合 + 账龄分桶)
     * 返回: customer_id, customer_code, customer_name, customer_type,
     *       total_billed, total_received, ar_outstanding,
     *       aging_0_7, aging_8_14, aging_15_30, aging_30_plus
     */
    @Select("""
            SELECT
              c.id AS customer_id, c.code AS customer_code,
              c.name AS customer_name, c.type AS customer_type,
              COALESCE(SUM(o.total_amount), 0) AS total_billed,
              COALESCE(SUM(p.received), 0) AS total_received,
              COALESCE(SUM(o.total_amount), 0) - COALESCE(SUM(p.received), 0) AS ar_outstanding,
              COALESCE(SUM(CASE WHEN DATEDIFF(CURDATE(), o.order_date) BETWEEN 0 AND 7
                                  THEN o.total_amount - COALESCE(p.received, 0)
                                  ELSE 0 END), 0) AS aging_0_7,
              COALESCE(SUM(CASE WHEN DATEDIFF(CURDATE(), o.order_date) BETWEEN 8 AND 14
                                  THEN o.total_amount - COALESCE(p.received, 0)
                                  ELSE 0 END), 0) AS aging_8_14,
              COALESCE(SUM(CASE WHEN DATEDIFF(CURDATE(), o.order_date) BETWEEN 15 AND 30
                                  THEN o.total_amount - COALESCE(p.received, 0)
                                  ELSE 0 END), 0) AS aging_15_30,
              COALESCE(SUM(CASE WHEN DATEDIFF(CURDATE(), o.order_date) > 30
                                  THEN o.total_amount - COALESCE(p.received, 0)
                                  ELSE 0 END), 0) AS aging_30_plus
              FROM customer c
              JOIN sales_order o ON o.customer_id = c.id AND o.deleted_at IS NULL
              LEFT JOIN (
                SELECT order_id, SUM(amount_kes) AS received
                  FROM payment
                 WHERE status IN ('cleared', 'partial')
                 GROUP BY order_id
              ) p ON p.order_id = o.id
              WHERE c.deleted_at IS NULL
                AND o.status NOT IN ('cancelled', 'returned')
              GROUP BY c.id, c.code, c.name, c.type
              HAVING ar_outstanding > 0
              ORDER BY ar_outstanding DESC
            """)
    List<Map<String, Object>> arAgingByCustomer();

    /**
     * 月度回款汇总
     */
    @Select("""
            SELECT
              DATE_FORMAT(payment_date, '%Y-%m') AS month,
              method,
              COALESCE(SUM(amount_kes), 0) AS total
              FROM payment
             WHERE payment_date >= #{from}
               AND payment_date <= #{to}
               AND status = 'cleared'
             GROUP BY DATE_FORMAT(payment_date, '%Y-%m'), method
             ORDER BY month DESC, method
            """)
    List<Map<String, Object>> monthlyPaymentRollup(@Param("from") LocalDate from,
                                                   @Param("to") LocalDate to);
}
