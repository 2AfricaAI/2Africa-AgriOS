package ai.toafrica.agrios.warehouse.mapper;

import ai.toafrica.agrios.warehouse.entity.WarehouseScrapItem;
import ai.toafrica.agrios.warehouse.vo.ScrapItemVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface WarehouseScrapItemMapper extends BaseMapper<WarehouseScrapItem> {
    @Select("""
            SELECT si.id, si.scrap_id, si.input_item_id,
              ii.code AS input_item_code, COALESCE(ii.name_en, ii.name) AS input_item_name, ii.unit,
              si.qty, si.reason
            FROM warehouse_scrap_item si
            JOIN input_item ii ON si.input_item_id = ii.id
            WHERE si.scrap_id = #{scrapId} ORDER BY si.id
            """)
    List<ScrapItemVO> findByScrapId(@Param("scrapId") Long scrapId);
}
