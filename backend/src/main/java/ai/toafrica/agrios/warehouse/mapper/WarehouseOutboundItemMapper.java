package ai.toafrica.agrios.warehouse.mapper;

import ai.toafrica.agrios.warehouse.entity.WarehouseOutboundItem;
import ai.toafrica.agrios.warehouse.vo.OutboundItemVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface WarehouseOutboundItemMapper extends BaseMapper<WarehouseOutboundItem> {

    @Select("""
            SELECT
              oi.id, oi.outbound_id, oi.input_item_id,
              ii.code AS input_item_code,
              COALESCE(ii.name_en, ii.name) AS input_item_name,
              ii.unit AS unit,
              oi.requested_qty, oi.picked_qty, oi.actual_qty, oi.remark
            FROM warehouse_outbound_item oi
            JOIN input_item ii ON oi.input_item_id = ii.id
            WHERE oi.outbound_id = #{outboundId}
            ORDER BY oi.id ASC
            """)
    List<OutboundItemVO> findByOutboundId(@Param("outboundId") Long outboundId);
}
