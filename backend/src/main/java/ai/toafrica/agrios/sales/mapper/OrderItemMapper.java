package ai.toafrica.agrios.sales.mapper;

import ai.toafrica.agrios.sales.entity.OrderItem;
import ai.toafrica.agrios.sales.vo.OrderItemVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    @Select("""
            SELECT
              oi.id, oi.order_id,
              oi.sku_id, sk.code AS sku_code, sk.name AS sku_name, sk.grade,
              c.name AS crop_name, v.name AS variety_name,
              sk.spec_id, ps.code AS spec_code, ps.name AS spec_name,
              oi.qty, oi.unit_price, oi.amount, oi.qty_shipped,
              oi.remark
              FROM order_item oi
              LEFT JOIN sku            sk ON oi.sku_id  = sk.id
              LEFT JOIN crop           c  ON sk.crop_id = c.id
              LEFT JOIN variety        v  ON sk.variety_id = v.id
              LEFT JOIN packaging_spec ps ON sk.spec_id = ps.id
              WHERE oi.order_id = #{orderId}
              ORDER BY oi.id ASC
            """)
    List<OrderItemVO> findByOrderId(@Param("orderId") Long orderId);
}
