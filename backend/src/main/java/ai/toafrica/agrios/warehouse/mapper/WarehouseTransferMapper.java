package ai.toafrica.agrios.warehouse.mapper;

import ai.toafrica.agrios.warehouse.entity.WarehouseTransfer;
import ai.toafrica.agrios.warehouse.vo.TransferVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.*;

@Mapper
public interface WarehouseTransferMapper extends BaseMapper<WarehouseTransfer> {
    @Select("""
            SELECT t.id, t.code, t.from_warehouse_id, fw.code AS from_warehouse_code, fw.name AS from_warehouse_name,
              t.to_warehouse_id, tw.code AS to_warehouse_code, tw.name AS to_warehouse_name,
              t.status, u.nickname AS confirmed_by_name, t.confirmed_at, t.remark, t.created_at,
              (SELECT COUNT(*) FROM warehouse_transfer_item ti WHERE ti.transfer_id = t.id) AS item_count
            FROM warehouse_transfer t
            JOIN location_warehouse fw ON t.from_warehouse_id = fw.id
            JOIN location_warehouse tw ON t.to_warehouse_id   = tw.id
            LEFT JOIN sys_user u ON t.confirmed_by = u.id
            ${ew.customSqlSegment}
            """)
    Page<TransferVO> pageWithJoin(Page<?> page, @Param("ew") QueryWrapper<TransferVO> wrapper);

    @Select("SELECT COUNT(*) FROM warehouse_transfer WHERE DATE(created_at) = #{date}")
    int countByDate(@Param("date") String date);
}
