package ai.toafrica.agrios.finance.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface MonthlyReportMapper {

    /**
     * 月度经营快照 (V2.0 文档承诺: 每月 5 日前出上月经营报表)
     * 一次性返回过去 N 个月每月: 收入 / 成本 / 毛利 / 订单数 / 客户数 / 回款
     */
    @Select("""
            SELECT
              months.month,
              COALESCE(rev.revenue, 0) AS revenue,
              COALESCE(cost.cost, 0) AS cost,
              COALESCE(rev.revenue, 0) - COALESCE(cost.cost, 0) AS gross_profit,
              COALESCE(ord.order_count, 0) AS order_count,
              COALESCE(ord.customer_count, 0) AS customer_count,
              COALESCE(pay.payment_received, 0) AS payment_received
              FROM (
                SELECT DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL n MONTH), '%Y-%m') AS month
                  FROM (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3
                        UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7
                        UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11) m
              ) months
              LEFT JOIN (
                SELECT DATE_FORMAT(recognition_date, '%Y-%m') AS month,
                       SUM(net_amount) AS revenue
                  FROM revenue WHERE status = 'recognized'
                 GROUP BY DATE_FORMAT(recognition_date, '%Y-%m')
              ) rev ON rev.month = months.month
              LEFT JOIN (
                SELECT DATE_FORMAT(occur_date, '%Y-%m') AS month,
                       SUM(labor_cost + water_cost + electricity_cost + fertilizer_cost + other_cost) AS cost
                  FROM activity
                 GROUP BY DATE_FORMAT(occur_date, '%Y-%m')
              ) cost ON cost.month = months.month
              LEFT JOIN (
                SELECT DATE_FORMAT(order_date, '%Y-%m') AS month,
                       COUNT(*) AS order_count,
                       COUNT(DISTINCT customer_id) AS customer_count
                  FROM sales_order WHERE deleted_at IS NULL
                 GROUP BY DATE_FORMAT(order_date, '%Y-%m')
              ) ord ON ord.month = months.month
              LEFT JOIN (
                SELECT DATE_FORMAT(payment_date, '%Y-%m') AS month,
                       SUM(amount_kes) AS payment_received
                  FROM payment WHERE status = 'cleared'
                 GROUP BY DATE_FORMAT(payment_date, '%Y-%m')
              ) pay ON pay.month = months.month
              ORDER BY months.month DESC
            """)
    List<Map<String, Object>> monthlySummary();
}
