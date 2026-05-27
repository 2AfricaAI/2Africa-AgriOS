package ai.toafrica.agrios.master.mapper;

import ai.toafrica.agrios.master.entity.InputStock;
import ai.toafrica.agrios.master.vo.InputStockVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * InputStock mapper (Sprint 22.2)
 */
@Mapper
public interface InputStockMapper extends BaseMapper<InputStock> {

    @Select("""
            SELECT
              s.id                              AS id,
              s.input_item_id                   AS input_item_id,
              ii.code                           AS input_item_code,
              COALESCE(ii.name_en, ii.name)     AS input_item_name,
              ii.input_type                     AS input_type,
              ii.unit                           AS unit,
              s.warehouse_id                    AS warehouse_id,
              w.code                            AS warehouse_code,
              w.name                            AS warehouse_name,
              s.qty_on_hand                     AS qty_on_hand,
              s.qty_reserved                    AS qty_reserved,
              (s.qty_on_hand - s.qty_reserved)  AS qty_available,
              s.last_stock_at                   AS last_stock_at
            FROM input_stock s
            JOIN input_item ii        ON s.input_item_id = ii.id
            JOIN location_warehouse w ON s.warehouse_id  = w.id
            ${ew.customSqlSegment}
            """)
    Page<InputStockVO> pageWithJoin(Page<?> page, @Param("ew") QueryWrapper<InputStockVO> wrapper);
}
