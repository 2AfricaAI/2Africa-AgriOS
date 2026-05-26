package ai.toafrica.agrios.procurement.mapper;

import ai.toafrica.agrios.procurement.entity.PurchaseOrderItem;
import ai.toafrica.agrios.procurement.vo.PurchaseOrderItemVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PurchaseOrderItemMapper extends BaseMapper<PurchaseOrderItem> {

    @Select("""
            SELECT
              id, po_id, input_type, description,
              quantity, unit, unit_price, amount, received_qty, remark
              FROM purchase_order_item
              WHERE po_id = #{poId}
              ORDER BY id ASC
            """)
    List<PurchaseOrderItemVO> findByPoId(@Param("poId") Long poId);
}
