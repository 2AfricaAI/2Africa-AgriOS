package ai.toafrica.agrios.sales.mapper;

import ai.toafrica.agrios.sales.entity.FulfillmentItem;
import ai.toafrica.agrios.sales.vo.FulfillmentItemVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FulfillmentItemMapper extends BaseMapper<FulfillmentItem> {

    @Select("""
            SELECT
              fi.id, fi.fulfillment_id, fi.order_item_id,
              fi.inventory_id, lw.code AS location_code, lw.name AS location_name,
              fi.batch_id, b.code AS batch_code, b.harvest_date AS batch_harvest_date,
              fi.sku_id, sk.code AS sku_code, sk.name AS sku_name, sk.grade,
              c.name AS crop_name, v.name AS variety_name, ps.name AS spec_name,
              fi.qty
              FROM fulfillment_item fi
              LEFT JOIN inventory          inv ON fi.inventory_id = inv.id
              LEFT JOIN location_warehouse lw  ON inv.location_id = lw.id
              LEFT JOIN batch              b   ON fi.batch_id     = b.id
              LEFT JOIN sku                sk  ON fi.sku_id       = sk.id
              LEFT JOIN crop               c   ON sk.crop_id      = c.id
              LEFT JOIN variety            v   ON sk.variety_id   = v.id
              LEFT JOIN packaging_spec     ps  ON sk.spec_id      = ps.id
              WHERE fi.fulfillment_id = #{fulfillmentId}
              ORDER BY fi.id ASC
            """)
    List<FulfillmentItemVO> findByFulfillmentId(@Param("fulfillmentId") Long fulfillmentId);
}
