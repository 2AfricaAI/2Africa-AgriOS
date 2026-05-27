package ai.toafrica.agrios.master.mapper;

import ai.toafrica.agrios.master.entity.InputStockLog;
import ai.toafrica.agrios.master.vo.InputStockLogVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface InputStockLogMapper extends BaseMapper<InputStockLog> {

    @Select("""
            SELECT
              l.id                              AS id,
              l.input_item_id                   AS input_item_id,
              ii.code                           AS input_item_code,
              COALESCE(ii.name_en, ii.name)     AS input_item_name,
              ii.unit                           AS unit,
              l.warehouse_id                    AS warehouse_id,
              w.code                            AS warehouse_code,
              w.name                            AS warehouse_name,
              l.direction                       AS direction,
              l.qty                             AS qty,
              l.reason_type                     AS reason_type,
              l.reference_type                  AS reference_type,
              l.reference_id                    AS reference_id,
              l.qty_after                       AS qty_after,
              l.operator_id                     AS operator_id,
              u.nickname                        AS operator_name,
              l.remark                          AS remark,
              l.created_at                      AS created_at
            FROM input_stock_log l
            JOIN input_item ii        ON l.input_item_id = ii.id
            JOIN location_warehouse w ON l.warehouse_id  = w.id
            LEFT JOIN sys_user u      ON l.operator_id   = u.id
            ${ew.customSqlSegment}
            """)
    Page<InputStockLogVO> pageWithJoin(Page<?> page, @Param("ew") QueryWrapper<InputStockLogVO> wrapper);
}
