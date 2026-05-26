package ai.toafrica.agrios.sales.mapper;

import ai.toafrica.agrios.sales.entity.SalesOrder;
import ai.toafrica.agrios.sales.vo.SalesOrderVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

@Mapper
public interface SalesOrderMapper extends BaseMapper<SalesOrder> {

    @Select("""
            SELECT
              o.id, o.code,
              o.customer_id, c.code AS customer_code, c.name AS customer_name, c.type AS customer_type,
              o.order_date, o.delivery_date, o.ship_to,
              o.currency, o.total_amount, o.status,
              o.payment_status, o.paid_amount, o.due_date,
              (SELECT COUNT(*) FROM order_item WHERE order_id = o.id) AS item_count,
              o.remark, o.created_at, o.updated_at
              FROM sales_order o
              LEFT JOIN customer c ON o.customer_id = c.id
              ${ew.customSqlSegment}
            """)
    IPage<SalesOrderVO> pageWithJoin(Page<SalesOrderVO> page,
                                     @Param("ew") QueryWrapper<SalesOrderVO> wrapper);

    @Select("SELECT COUNT(*) FROM sales_order WHERE order_date = #{date} AND deleted_at IS NULL")
    int countByDate(@Param("date") LocalDate date);
}
