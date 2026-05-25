package ai.toafrica.agrios.production.mapper;

import ai.toafrica.agrios.production.entity.HarvestRecord;
import ai.toafrica.agrios.production.vo.HarvestRow;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

@Mapper
public interface HarvestRecordMapper extends BaseMapper<HarvestRecord> {

    @Select("""
            SELECT
              h.id, h.code, h.client_uuid,
              h.plot_id, p.code AS plot_code, p.name AS plot_name,
              h.plan_id, pp.code AS plan_code,
              h.crop_id, c.name AS crop_name,
              h.variety_id, v.name AS variety_name,
              h.batch_id, b.code AS batch_code,
              h.harvest_date, h.qty_kg,
              h.operator_id, op.nickname AS operator_name,
              h.photos AS photos_json,
              h.remark, h.created_at, h.updated_at
              FROM harvest_record h
              LEFT JOIN plot          p  ON h.plot_id     = p.id
              LEFT JOIN planting_plan pp ON h.plan_id     = pp.id
              LEFT JOIN crop          c  ON h.crop_id     = c.id
              LEFT JOIN variety       v  ON h.variety_id  = v.id
              LEFT JOIN batch         b  ON h.batch_id    = b.id
              LEFT JOIN sys_user      op ON h.operator_id = op.id
              ${ew.customSqlSegment}
            """)
    IPage<HarvestRow> pageWithJoin(Page<HarvestRow> page,
                                   @Param("ew") QueryWrapper<HarvestRow> wrapper);

    @Select("SELECT id FROM harvest_record WHERE client_uuid = #{uuid} LIMIT 1")
    Long findIdByClientUuid(@Param("uuid") String uuid);

    /** 同日已有多少条采收 - 用于生成下一个序号 */
    @Select("SELECT COUNT(*) FROM harvest_record WHERE harvest_date = #{date}")
    int countByDate(@Param("date") LocalDate date);
}
