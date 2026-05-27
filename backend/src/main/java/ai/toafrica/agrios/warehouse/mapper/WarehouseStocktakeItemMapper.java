package ai.toafrica.agrios.warehouse.mapper;

import ai.toafrica.agrios.warehouse.entity.WarehouseStocktakeItem;
import ai.toafrica.agrios.warehouse.vo.StocktakeItemVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface WarehouseStocktakeItemMapper extends BaseMapper<WarehouseStocktakeItem> {

    @Select("""
            SELECT si.id, si.stocktake_id, si.input_item_id,
              ii.code AS input_item_code, COALESCE(ii.name_en, ii.name) AS input_item_name,
              ii.unit AS unit,
              si.system_qty, si.count_qty, si.diff_qty, si.remark
            FROM warehouse_stocktake_item si
            JOIN input_item ii ON si.input_item_id = ii.id
            WHERE si.stocktake_id = #{stocktakeId}
            ORDER BY si.id ASC
            """)
    List<StocktakeItemVO> findByStocktakeId(@Param("stocktakeId") Long stocktakeId);
}
