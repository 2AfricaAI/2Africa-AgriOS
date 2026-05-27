package ai.toafrica.agrios.warehouse.mapper;

import ai.toafrica.agrios.warehouse.entity.WarehouseStocktake;
import ai.toafrica.agrios.warehouse.vo.StocktakeVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.*;

@Mapper
public interface WarehouseStocktakeMapper extends BaseMapper<WarehouseStocktake> {

    @Select("""
            SELECT s.id, s.code, s.warehouse_id, w.code AS warehouse_code, w.name AS warehouse_name,
              s.count_type, s.status,
              cu.nickname AS counted_by_name, s.counted_at,
              fu.nickname AS confirmed_by_name, s.confirmed_at,
              s.remark, s.created_at,
              (SELECT COUNT(*) FROM warehouse_stocktake_item si WHERE si.stocktake_id = s.id) AS item_count
            FROM warehouse_stocktake s
            JOIN location_warehouse w ON s.warehouse_id = w.id
            LEFT JOIN sys_user cu ON s.counted_by = cu.id
            LEFT JOIN sys_user fu ON s.confirmed_by = fu.id
            ${ew.customSqlSegment}
            """)
    Page<StocktakeVO> pageWithJoin(Page<?> page, @Param("ew") QueryWrapper<StocktakeVO> wrapper);

    @Select("SELECT COUNT(*) FROM warehouse_stocktake WHERE DATE(created_at) = #{date}")
    int countByDate(@Param("date") String date);
}
