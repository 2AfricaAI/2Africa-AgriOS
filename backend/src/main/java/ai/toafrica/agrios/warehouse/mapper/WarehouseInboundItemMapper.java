package ai.toafrica.agrios.warehouse.mapper;

import ai.toafrica.agrios.warehouse.entity.WarehouseInboundItem;
import ai.toafrica.agrios.warehouse.vo.InboundItemVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WarehouseInboundItemMapper extends BaseMapper<WarehouseInboundItem> {

    @Select("""
            SELECT
              wi.id, wi.inbound_id, wi.input_item_id,
              ii.code       AS input_item_code,
              COALESCE(ii.name_en, ii.name) AS input_item_name,
              ii.unit        AS unit,
              wi.expected_qty, wi.actual_qty, wi.remark
            FROM warehouse_inbound_item wi
            JOIN input_item ii ON wi.input_item_id = ii.id
            WHERE wi.inbound_id = #{inboundId}
            ORDER BY wi.id ASC
            """)
    List<InboundItemVO> findByInboundId(@Param("inboundId") Long inboundId);
}
