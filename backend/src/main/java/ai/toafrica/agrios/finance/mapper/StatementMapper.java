package ai.toafrica.agrios.finance.mapper;

import ai.toafrica.agrios.finance.vo.StatementOrderLine;
import ai.toafrica.agrios.finance.vo.StatementPaymentLine;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 客户对账单 SQL - Sprint 16.4.
 *   所有金额返回 KES 本位币.
 */
@Mapper
public interface StatementMapper {

    /**
     * 期初余额 = SUM(period 前的订单 total_amount, KES) - SUM(period 前的 cleared/partial payment, KES)
     */
    @Select("""
            SELECT
              (SELECT COALESCE(SUM(total_amount), 0)
                 FROM sales_order
                WHERE customer_id = #{customerId}
                  AND deleted_at IS NULL
                  AND status NOT IN ('cancelled', 'returned')
                  AND order_date < #{from})
              -
              (SELECT COALESCE(SUM(p.amount_kes), 0)
                 FROM payment p
                 JOIN sales_order o ON p.order_id = o.id
                WHERE p.customer_id = #{customerId}
                  AND p.status IN ('cleared', 'partial')
                  AND p.payment_date < #{from}
                  AND o.deleted_at IS NULL
                  AND o.status NOT IN ('cancelled', 'returned'))
              AS opening_balance
            """)
    BigDecimal openingBalance(@Param("customerId") Long customerId,
                              @Param("from")       LocalDate from);

    /**
     * 期间订单明细
     */
    @Select("""
            SELECT
              id AS order_id, code AS order_code,
              order_date, due_date,
              total_amount, paid_amount,
              GREATEST(total_amount - paid_amount, 0) AS outstanding,
              currency, status, payment_status
              FROM sales_order
              WHERE customer_id = #{customerId}
                AND deleted_at IS NULL
                AND status NOT IN ('cancelled', 'returned')
                AND order_date BETWEEN #{from} AND #{to}
              ORDER BY order_date ASC, id ASC
            """)
    List<StatementOrderLine> periodOrders(@Param("customerId") Long customerId,
                                          @Param("from")       LocalDate from,
                                          @Param("to")         LocalDate to);

    /**
     * 期间收款明细
     */
    @Select("""
            SELECT
              p.id AS payment_id, p.code AS payment_code,
              p.payment_date, p.amount, p.currency, p.amount_kes,
              p.method, p.reference_no,
              p.order_id, o.code AS order_code
              FROM payment p
              JOIN sales_order o ON p.order_id = o.id
              WHERE p.customer_id = #{customerId}
                AND p.status IN ('cleared', 'partial')
                AND p.payment_date BETWEEN #{from} AND #{to}
              ORDER BY p.payment_date ASC, p.id ASC
            """)
    List<StatementPaymentLine> periodPayments(@Param("customerId") Long customerId,
                                              @Param("from")       LocalDate from,
                                              @Param("to")         LocalDate to);

    /**
     * 账龄 (截至 asOf 日期, 按未结清部分分桶)
     *   bucket = DATEDIFF(asOf, order_date)
     *   outstanding_at_asOf = total_amount - SUM(payment.amount_kes WHERE payment_date <= asOf)
     */
    @Select("""
            SELECT
              COALESCE(SUM(CASE WHEN DATEDIFF(#{asOf}, o.order_date) BETWEEN 0 AND 7
                                  THEN GREATEST(o.total_amount - COALESCE(p.received_up_to, 0), 0)
                                  ELSE 0 END), 0) AS aging_0_7,
              COALESCE(SUM(CASE WHEN DATEDIFF(#{asOf}, o.order_date) BETWEEN 8 AND 14
                                  THEN GREATEST(o.total_amount - COALESCE(p.received_up_to, 0), 0)
                                  ELSE 0 END), 0) AS aging_8_14,
              COALESCE(SUM(CASE WHEN DATEDIFF(#{asOf}, o.order_date) BETWEEN 15 AND 30
                                  THEN GREATEST(o.total_amount - COALESCE(p.received_up_to, 0), 0)
                                  ELSE 0 END), 0) AS aging_15_30,
              COALESCE(SUM(CASE WHEN DATEDIFF(#{asOf}, o.order_date) > 30
                                  THEN GREATEST(o.total_amount - COALESCE(p.received_up_to, 0), 0)
                                  ELSE 0 END), 0) AS aging_30_plus
              FROM sales_order o
              LEFT JOIN (
                SELECT order_id, SUM(amount_kes) AS received_up_to
                  FROM payment
                 WHERE status IN ('cleared', 'partial')
                   AND payment_date <= #{asOf}
                 GROUP BY order_id
              ) p ON p.order_id = o.id
              WHERE o.customer_id = #{customerId}
                AND o.deleted_at IS NULL
                AND o.status NOT IN ('cancelled', 'returned')
                AND o.order_date <= #{asOf}
                AND (o.total_amount - COALESCE(p.received_up_to, 0)) > 0
            """)
    Map<String, Object> agingAsOf(@Param("customerId") Long customerId,
                                  @Param("asOf")       LocalDate asOf);
}
