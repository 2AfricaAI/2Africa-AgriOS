package ai.toafrica.agrios.packhouse.mapper;

import ai.toafrica.agrios.packhouse.entity.Packing;
import ai.toafrica.agrios.packhouse.vo.PackingRow;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

@Mapper
public interface PackingMapper extends BaseMapper<Packing> {

    @Select("""
            SELECT
              pk.id, pk.code,
              pk.batch_id, b.code AS batch_code,
              pk.grade,
              pk.spec_id, ps.code AS spec_code, ps.name AS spec_name,
              pk.sku_id, sk.code AS sku_code, sk.name AS sku_name,
              pk.qty_units, pk.net_weight_kg,
              pk.location_id, lw.code AS location_code, lw.name AS location_name,
              pk.packed_at,
              pk.operator_id, op.nickname AS operator_name,
              pk.remark, pk.created_at,
              c.name AS crop_name
              FROM packing pk
              LEFT JOIN batch              b  ON pk.batch_id    = b.id
              LEFT JOIN packaging_spec     ps ON pk.spec_id     = ps.id
              LEFT JOIN sku                sk ON pk.sku_id      = sk.id
              LEFT JOIN location_warehouse lw ON pk.location_id = lw.id
              LEFT JOIN sys_user           op ON pk.operator_id = op.id
              LEFT JOIN crop               c  ON sk.crop_id     = c.id
              ${ew.customSqlSegment}
            """)
    IPage<PackingRow> pageWithJoin(Page<PackingRow> page,
                                   @Param("ew") QueryWrapper<PackingRow> wrapper);

    @Select("SELECT COUNT(*) FROM packing WHERE DATE(packed_at) = #{date}")
    int countByDate(@Param("date") LocalDate date);
}
