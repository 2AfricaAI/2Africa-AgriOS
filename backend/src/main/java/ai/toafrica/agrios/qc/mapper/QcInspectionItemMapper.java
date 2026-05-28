package ai.toafrica.agrios.qc.mapper;

import ai.toafrica.agrios.qc.entity.QcInspectionItem;
import ai.toafrica.agrios.qc.vo.QcInspectionItemVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface QcInspectionItemMapper extends BaseMapper<QcInspectionItem> {

    @Select("""
            SELECT id, inspection_id, check_point, expected_value, actual_value, result, remark
            FROM qc_inspection_item WHERE inspection_id = #{inspectionId} ORDER BY id
            """)
    List<QcInspectionItemVO> findByInspectionId(@Param("inspectionId") Long inspectionId);
}
