package ai.toafrica.agrios.warehouse.mapper;

import ai.toafrica.agrios.warehouse.entity.WarehouseOutbound;
import ai.toafrica.agrios.warehouse.vo.OutboundVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.*;

@Mapper
public interface WarehouseOutboundMapper extends BaseMapper<WarehouseOutbound> {

    @Select("""
            SELECT
              o.id, o.code, o.source_type, o.source_id,
              o.warehouse_id,
              w.code  AS warehouse_code,
              w.name  AS warehouse_name,
              o.status,
              o.picked_by, pu.nickname AS picked_by_name, o.picked_at,
              o.confirmed_by, cu.nickname AS confirmed_by_name, o.confirmed_at,
              o.remark, o.created_at,
              (SELECT COUNT(*) FROM warehouse_outbound_item oi WHERE oi.outbound_id = o.id) AS item_count
            FROM warehouse_outbound o
            JOIN location_warehouse w ON o.warehouse_id = w.id
            LEFT JOIN sys_user pu ON o.picked_by = pu.id
            LEFT JOIN sys_user cu ON o.confirmed_by = cu.id
            ${ew.customSqlSegment}
            """)
    Page<OutboundVO> pageWithJoin(Page<?> page, @Param("ew") QueryWrapper<OutboundVO> wrapper);

    @Select("SELECT COUNT(*) FROM warehouse_outbound WHERE DATE(created_at) = #{date}")
    int countByDate(@Param("date") String date);
}
