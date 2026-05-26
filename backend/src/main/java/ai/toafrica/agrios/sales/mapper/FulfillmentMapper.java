package ai.toafrica.agrios.sales.mapper;

import ai.toafrica.agrios.sales.entity.Fulfillment;
import ai.toafrica.agrios.sales.vo.FulfillmentVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

@Mapper
public interface FulfillmentMapper extends BaseMapper<Fulfillment> {

    @Select("""
            SELECT
              f.id, f.code,
              f.order_id, o.code AS order_code,
              o.customer_id, c.name AS customer_name, c.code AS customer_code,
              f.picker_id, u.nickname AS picker_name,
              f.plan_ship_at, f.ship_at, f.delivered_at,
              f.status, f.ship_method, f.track_no,
              f.driver_name, f.driver_phone, f.vehicle_no,
              f.remark,
              (SELECT COUNT(*) FROM fulfillment_item WHERE fulfillment_id = f.id) AS item_count,
              f.created_at, f.updated_at
              FROM fulfillment f
              LEFT JOIN sales_order o ON f.order_id    = o.id
              LEFT JOIN customer    c ON o.customer_id = c.id
              LEFT JOIN sys_user    u ON f.picker_id   = u.id
              ${ew.customSqlSegment}
            """)
    IPage<FulfillmentVO> pageWithJoin(Page<FulfillmentVO> page,
                                      @Param("ew") QueryWrapper<FulfillmentVO> wrapper);

    @Select("SELECT COUNT(*) FROM fulfillment WHERE DATE(created_at) = #{date}")
    int countByDate(@Param("date") LocalDate date);
}
