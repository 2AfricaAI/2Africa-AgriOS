package ai.toafrica.agrios.warehouse.mapper;

import ai.toafrica.agrios.warehouse.entity.WarehouseTransferItem;
import ai.toafrica.agrios.warehouse.vo.TransferItemVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface WarehouseTransferItemMapper extends BaseMapper<WarehouseTransferItem> {
    @Select("""
            SELECT ti.id, ti.transfer_id, ti.input_item_id,
              ii.code AS input_item_code, COALESCE(ii.name_en, ii.name) AS input_item_name, ii.unit AS unit,
              ti.qty, ti.remark
            FROM warehouse_transfer_item ti
            JOIN input_item ii ON ti.input_item_id = ii.id
            WHERE ti.transfer_id = #{transferId} ORDER BY ti.id
            """)
    List<TransferItemVO> findByTransferId(@Param("transferId") Long transferId);
}
