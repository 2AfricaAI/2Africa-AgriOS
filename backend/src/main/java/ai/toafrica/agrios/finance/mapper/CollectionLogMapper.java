package ai.toafrica.agrios.finance.mapper;

import ai.toafrica.agrios.finance.entity.CollectionLog;
import ai.toafrica.agrios.finance.vo.CollectionLogVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface CollectionLogMapper extends BaseMapper<CollectionLog> {

    /**
     * 列表分页 (含客户 / 订单 join)
     */
    @Select("""
            SELECT
              l.id,
              l.customer_id, c.code AS customer_code, c.name AS customer_name,
              l.order_id,    o.code AS order_code,
              l.log_date, l.channel, l.contact_person, l.outcome,
              l.promised_date, l.promised_amount,
              l.content, l.next_action_date,
              l.operator_id, l.operator_name,
              l.created_at
              FROM collection_log l
              LEFT JOIN customer c    ON l.customer_id = c.id
              LEFT JOIN sales_order o ON l.order_id    = o.id
              ${ew.customSqlSegment}
            """)
    IPage<CollectionLogVO> pageWithJoin(Page<CollectionLogVO> page,
                                        @Param("ew") QueryWrapper<CollectionLogVO> wrapper);

    /**
     * 按客户查询全部跟催 (用于客户详情 / AR 台账时间线)
     */
    @Select("""
            SELECT
              l.id,
              l.customer_id, c.code AS customer_code, c.name AS customer_name,
              l.order_id,    o.code AS order_code,
              l.log_date, l.channel, l.contact_person, l.outcome,
              l.promised_date, l.promised_amount,
              l.content, l.next_action_date,
              l.operator_id, l.operator_name,
              l.created_at
              FROM collection_log l
              LEFT JOIN customer c    ON l.customer_id = c.id
              LEFT JOIN sales_order o ON l.order_id    = o.id
              WHERE l.customer_id = #{customerId}
                AND l.deleted_at IS NULL
              ORDER BY l.log_date DESC, l.id DESC
            """)
    List<CollectionLogVO> findByCustomer(@Param("customerId") Long customerId);

    /**
     * 查当前生效的承诺还款 (用于现金流预测)
     *   - outcome = promised
     *   - promised_date >= today
     *   - 该订单尚未结清 (paid_amount < total_amount)
     */
    @Select("""
            SELECT
              l.id, l.customer_id, l.order_id,
              l.promised_date, l.promised_amount,
              o.code AS order_code, c.name AS customer_name
              FROM collection_log l
              JOIN sales_order o ON l.order_id = o.id AND o.deleted_at IS NULL
              JOIN customer    c ON l.customer_id = c.id
              WHERE l.deleted_at IS NULL
                AND l.outcome = 'promised'
                AND l.promised_date >= #{today}
                AND o.payment_status <> 'paid'
              ORDER BY l.promised_date ASC
            """)
    List<java.util.Map<String, Object>> findActivePromises(@Param("today") LocalDate today);
}
