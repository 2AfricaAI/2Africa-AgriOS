package ai.toafrica.agrios.finance.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 现金流预测 SQL - Sprint 18.1.
 *   3 个独立查询: AR / 承诺还款 / AP
 *   返回 Map 让 Service 层灵活处理
 */
@Mapper
public interface CashFlowMapper {

    /**
     * 应收账款到期 (Inflow)
     *   - 销售订单未付清
     *   - due_date 在 [from, to] 窗口内
     *   - 排除已被 collection_log promise 覆盖的订单
     * 返回: order_id, order_code, due_date, customer_name, outstanding
     */
    @Select("""
            SELECT
              o.id           AS order_id,
              o.code         AS order_code,
              o.due_date     AS due_date,
              c.name         AS customer_name,
              GREATEST(o.total_amount - o.paid_amount, 0) AS outstanding
              FROM sales_order o
              JOIN customer c ON o.customer_id = c.id
              WHERE o.deleted_at IS NULL
                AND o.payment_status <> 'paid'
                AND o.status NOT IN ('cancelled', 'returned')
                AND o.due_date BETWEEN #{from} AND #{to}
                AND o.total_amount > o.paid_amount
                AND o.id NOT IN (
                  SELECT DISTINCT cl.order_id
                    FROM collection_log cl
                   WHERE cl.deleted_at IS NULL
                     AND cl.outcome = 'promised'
                     AND cl.order_id IS NOT NULL
                     AND cl.promised_date >= #{today}
                     AND cl.promised_date <= #{to}
                )
              ORDER BY o.due_date ASC
            """)
    List<Map<String, Object>> arDueInWindow(@Param("from")  LocalDate from,
                                            @Param("to")    LocalDate to,
                                            @Param("today") LocalDate today);

    /**
     * 客户承诺还款 (Inflow, 覆盖 AR due_date)
     *   - collection_log.outcome=promised
     *   - promised_date 在 [today, to] 窗口
     *   - 对应订单未付清 (或承诺无关联订单)
     *   - 取 MAX(promised_date) per order (同一订单多次承诺取最新)
     * 返回: log_id, customer_name, order_code, promised_date, amount
     */
    @Select("""
            SELECT
              cl.id            AS log_id,
              cl.customer_id   AS customer_id,
              c.name           AS customer_name,
              cl.order_id      AS order_id,
              o.code           AS order_code,
              cl.promised_date AS promised_date,
              COALESCE(cl.promised_amount,
                       GREATEST(o.total_amount - o.paid_amount, 0)) AS amount
              FROM collection_log cl
              JOIN customer    c ON cl.customer_id = c.id
              LEFT JOIN sales_order o ON cl.order_id = o.id AND o.deleted_at IS NULL
              WHERE cl.deleted_at IS NULL
                AND cl.outcome = 'promised'
                AND cl.promised_date >= #{today}
                AND cl.promised_date <= #{to}
                AND (o.id IS NULL OR (o.payment_status <> 'paid' AND o.status NOT IN ('cancelled','returned')))
                AND cl.id IN (
                  -- 同一 order 取最新承诺
                  SELECT MAX(id) FROM collection_log
                  WHERE deleted_at IS NULL AND outcome='promised'
                    AND promised_date >= #{today} AND promised_date <= #{to}
                  GROUP BY COALESCE(order_id, -id)
                )
              ORDER BY cl.promised_date ASC
            """)
    List<Map<String, Object>> promisesInWindow(@Param("today") LocalDate today,
                                               @Param("to")    LocalDate to);

    /**
     * 应付账款到期 (Outflow)
     * 返回: po_id, po_code, due_date, supplier_name, outstanding
     */
    @Select("""
            SELECT
              p.id        AS po_id,
              p.code      AS po_code,
              p.due_date  AS due_date,
              s.name      AS supplier_name,
              GREATEST(p.total_amount - p.paid_amount, 0) AS outstanding
              FROM purchase_order p
              JOIN supplier s ON p.supplier_id = s.id
              WHERE p.deleted_at IS NULL
                AND p.payment_status <> 'paid'
                AND p.status NOT IN ('cancelled')
                AND p.due_date BETWEEN #{from} AND #{to}
                AND p.total_amount > p.paid_amount
              ORDER BY p.due_date ASC
            """)
    List<Map<String, Object>> apDueInWindow(@Param("from") LocalDate from,
                                            @Param("to")   LocalDate to);
}
