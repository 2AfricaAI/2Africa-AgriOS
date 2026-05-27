package ai.toafrica.agrios.warehouse.mapper;

import ai.toafrica.agrios.warehouse.entity.WarehouseScrap;
import ai.toafrica.agrios.warehouse.vo.ScrapVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.*;

@Mapper
public interface WarehouseScrapMapper extends BaseMapper<WarehouseScrap> {
    @Select("""
            SELECT s.id, s.code, s.warehouse_id, w.code AS warehouse_code, w.name AS warehouse_name,
              s.scrap_type, s.status, u.nickname AS confirmed_by_name, s.confirmed_at,
              s.remark, s.created_at,
              (SELECT COUNT(*) FROM warehouse_scrap_item si WHERE si.scrap_id = s.id) AS item_count
            FROM warehouse_scrap s
            JOIN location_warehouse w ON s.warehouse_id = w.id
            LEFT JOIN sys_user u ON s.confirmed_by = u.id
            ${ew.customSqlSegment}
            """)
    Page<ScrapVO> pageWithJoin(Page<?> page, @Param("ew") QueryWrapper<ScrapVO> wrapper);

    @Select("SELECT COUNT(*) FROM warehouse_scrap WHERE DATE(created_at) = #{date}")
    int countByDate(@Param("date") String date);
}
