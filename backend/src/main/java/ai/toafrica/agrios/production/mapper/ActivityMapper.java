package ai.toafrica.agrios.production.mapper;

import ai.toafrica.agrios.production.entity.Activity;
import ai.toafrica.agrios.production.vo.ActivityRow;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Activity Mapper
 *
 * pageWithJoin 的列别名都是 snake_case,跟 ActivityRow 的 camelCase 字段
 * 通过 application.yml 中 map-underscore-to-camel-case=true 自动映射,
 * 不需要 @Results 显式声明。
 */
@Mapper
public interface ActivityMapper extends BaseMapper<Activity> {

    @Select("""
            SELECT
              a.id,
              a.client_uuid,
              a.plot_id, p.code AS plot_code, p.name AS plot_name,
              a.plan_id, pp.code AS plan_code,
              a.activity_type, a.occur_date,
              a.operator_id, op.nickname AS operator_name,
              a.photos AS photos_json,
              a.location_gps, a.remark,
              a.labor_cost, a.water_cost, a.electricity_cost, a.fertilizer_cost, a.other_cost, a.cost_currency,
              a.labor_po_item_id, a.water_po_item_id, a.electricity_po_item_id, a.fertilizer_po_item_id, a.other_po_item_id,
              a.audit_status, a.auditor_id, au.nickname AS auditor_name,
              a.audited_at, a.audit_remark,
              a.created_at, a.updated_at
              FROM activity a
              LEFT JOIN plot          p  ON a.plot_id     = p.id
              LEFT JOIN planting_plan pp ON a.plan_id     = pp.id
              LEFT JOIN sys_user      op ON a.operator_id = op.id
              LEFT JOIN sys_user      au ON a.auditor_id  = au.id
             ${ew.customSqlSegment}
            """)
    IPage<ActivityRow> pageWithJoin(Page<ActivityRow> page,
                                    @Param("ew") QueryWrapper<ActivityRow> wrapper);

    /** 按 client_uuid 查重(幂等性校验) */
    @Select("SELECT id FROM activity WHERE client_uuid = #{uuid} LIMIT 1")
    Long findIdByClientUuid(@Param("uuid") String uuid);
}
