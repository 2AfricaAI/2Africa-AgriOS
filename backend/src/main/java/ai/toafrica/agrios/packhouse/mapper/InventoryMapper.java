package ai.toafrica.agrios.packhouse.mapper;

import ai.toafrica.agrios.packhouse.entity.Inventory;
import ai.toafrica.agrios.packhouse.vo.InventoryRow;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {

    @Select("""
            SELECT
              i.id,
              i.sku_id, sk.code AS sku_code, sk.name AS sku_name,
              i.batch_id, b.code AS batch_code,
              i.grade,
              i.location_id, lw.code AS location_code, lw.name AS location_name,
              i.qty_avail, i.qty_locked, i.qty_in_transit,
              i.unit, i.prod_date, i.status,
              i.last_op_at, i.updated_at,
              c.name AS crop_name,
              v.name AS variety_name
              FROM inventory i
              LEFT JOIN sku                sk ON i.sku_id      = sk.id
              LEFT JOIN batch              b  ON i.batch_id    = b.id
              LEFT JOIN location_warehouse lw ON i.location_id = lw.id
              LEFT JOIN crop               c  ON sk.crop_id    = c.id
              LEFT JOIN variety            v  ON sk.variety_id = v.id
              ${ew.customSqlSegment}
            """)
    IPage<InventoryRow> pageWithJoin(Page<InventoryRow> page,
                                     @Param("ew") QueryWrapper<InventoryRow> wrapper);

    /**
     * 拣货 FIFO 查询: 按生产日期升序 (旧批先出), id 升序 tie-break.
     * 只返回 status=normal 且 qty_avail>0 的库存行.
     */
    @Select("""
            SELECT * FROM inventory
            WHERE sku_id = #{skuId}
              AND status = 'normal'
              AND qty_avail > 0
            ORDER BY prod_date ASC, id ASC
            """)
    List<Inventory> findAvailableBySkuFifo(@Param("skuId") Long skuId);
}
