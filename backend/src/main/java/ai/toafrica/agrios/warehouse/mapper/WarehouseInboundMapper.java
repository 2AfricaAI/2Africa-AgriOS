package ai.toafrica.agrios.warehouse.mapper;

import ai.toafrica.agrios.warehouse.entity.WarehouseInbound;
import ai.toafrica.agrios.warehouse.vo.InboundVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WarehouseInboundMapper extends BaseMapper<WarehouseInbound> {

    @Select("""
            SELECT
              i.id, i.code, i.source_type, i.source_id,
              po.code           AS source_code,
              i.warehouse_id,
              w.code             AS warehouse_code,
              w.name             AS warehouse_name,
              i.status,
              i.confirmed_by,
              u.nickname         AS confirmed_by_name,
              i.confirmed_at,
              i.remark,
              i.created_at,
              (SELECT COUNT(*) FROM warehouse_inbound_item ii WHERE ii.inbound_id = i.id) AS item_count
            FROM warehouse_inbound i
            JOIN location_warehouse w ON i.warehouse_id = w.id
            LEFT JOIN purchase_order po ON i.source_type = 'po_receive' AND i.source_id = po.id
            LEFT JOIN sys_user u ON i.confirmed_by = u.id
            ${ew.customSqlSegment}
            """)
    Page<InboundVO> pageWithJoin(Page<?> page, @Param("ew") QueryWrapper<InboundVO> wrapper);

    @Select("SELECT COUNT(*) FROM warehouse_inbound WHERE DATE(created_at) = #{date}")
    int countByDate(@Param("date") String date);
}
