package ai.toafrica.agrios.qc.mapper;

import ai.toafrica.agrios.qc.entity.QcInspection;
import ai.toafrica.agrios.qc.vo.QcInspectionVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface QcInspectionMapper extends BaseMapper<QcInspection> {

    @Select("""
            SELECT q.id, q.code, q.inspection_type, q.ref_type, q.ref_id, q.ref_code,
              q.inspect_date,
              q.inspector_id, u.nickname AS inspector_name,
              q.result, q.result_remark, q.photo_ids, q.remark, q.created_at,
              (SELECT COUNT(*) FROM qc_inspection_item qi WHERE qi.inspection_id = q.id) AS item_count
            FROM qc_inspection q
            LEFT JOIN sys_user u ON q.inspector_id = u.id
            ${ew.customSqlSegment}
            """)
    Page<QcInspectionVO> pageWithJoin(Page<?> page, @Param("ew") QueryWrapper<QcInspectionVO> wrapper);

    @Select("SELECT COUNT(*) FROM qc_inspection WHERE DATE(created_at) = #{date}")
    int countByDate(@Param("date") String date);
}
