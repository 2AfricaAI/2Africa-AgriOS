package ai.toafrica.agrios.production.mapper;

import ai.toafrica.agrios.production.entity.PlantingPlan;
import ai.toafrica.agrios.production.vo.PlantingPlanVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PlantingPlanMapper extends BaseMapper<PlantingPlan> {

    /**
     * 分页查询 - JOIN 出 plot/crop/variety 名称
     *
     * 注意:
     *  - customSqlSegment 在 wrapper 非空时会自动加 WHERE 前缀;
     *    所以 SQL 模板里不能再写 WHERE,否则会出现双 WHERE 语法错误。
     *  - 软删过滤(pp.deleted_at IS NULL) 由 Service 层放进 wrapper。
     */
    @Select("""
            SELECT
              pp.id, pp.code,
              pp.plot_id, p.code AS plot_code, p.name AS plot_name,
              pp.crop_id, c.code AS crop_code, c.name AS crop_name,
              pp.variety_id, v.code AS variety_code, v.name AS variety_name,
              pp.area_mu, pp.plan_start_date, pp.plan_harvest_date,
              pp.actual_start_date, pp.actual_finish_date,
              pp.target_yield_kg, pp.status, pp.remark,
              pp.created_at, pp.updated_at
              FROM planting_plan pp
              LEFT JOIN plot    p ON pp.plot_id    = p.id
              LEFT JOIN crop    c ON pp.crop_id    = c.id
              LEFT JOIN variety v ON pp.variety_id = v.id
             ${ew.customSqlSegment}
            """)
    IPage<PlantingPlanVO> pageWithJoin(Page<PlantingPlanVO> page,
                                       @Param("ew") QueryWrapper<PlantingPlanVO> wrapper);
}
