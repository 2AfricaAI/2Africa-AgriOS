package ai.toafrica.agrios.production.mapper;

import ai.toafrica.agrios.production.entity.Batch;
import ai.toafrica.agrios.production.vo.BatchVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

@Mapper
public interface BatchMapper extends BaseMapper<Batch> {

    @Select("""
            SELECT
              b.id, b.code,
              b.parent_batch_id, pb.code AS parent_batch_code,
              b.plot_id, p.code AS plot_code, p.name AS plot_name,
              b.plan_id, pp.code AS plan_code,
              b.crop_id, c.name AS crop_name,
              b.variety_id, v.name AS variety_name,
              b.harvest_record_id, h.code AS harvest_record_code,
              b.harvest_date, b.qty_kg, b.qty_remain_kg,
              b.status, b.remark,
              b.created_at, b.updated_at
              FROM batch b
              LEFT JOIN batch          pb ON b.parent_batch_id   = pb.id
              LEFT JOIN plot           p  ON b.plot_id           = p.id
              LEFT JOIN planting_plan  pp ON b.plan_id           = pp.id
              LEFT JOIN crop           c  ON b.crop_id           = c.id
              LEFT JOIN variety        v  ON b.variety_id        = v.id
              LEFT JOIN harvest_record h  ON b.harvest_record_id = h.id
             ${ew.customSqlSegment}
            """)
    IPage<BatchVO> pageWithJoin(Page<BatchVO> page,
                                @Param("ew") QueryWrapper<BatchVO> wrapper);

    /** 同日同地块已有多少 batch - 生成下一个序号 */
    @Select("SELECT COUNT(*) FROM batch WHERE harvest_date = #{date} AND plot_id = #{plotId} AND deleted_at IS NULL")
    int countByDateAndPlot(@Param("date") LocalDate date, @Param("plotId") Long plotId);
}
