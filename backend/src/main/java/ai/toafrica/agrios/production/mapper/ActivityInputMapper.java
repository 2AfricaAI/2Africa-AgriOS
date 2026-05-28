package ai.toafrica.agrios.production.mapper;

import ai.toafrica.agrios.production.entity.ActivityInput;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ActivityInputMapper extends BaseMapper<ActivityInput> {

    @Select("""
            SELECT ai.id, ai.activity_id, ai.input_id AS input_item_id,
              ii.code AS input_item_code,
              COALESCE(ii.name_en, ii.name) AS input_item_name,
              ii.input_type AS input_type,
              ii.phi_days AS phi_days,
              ai.qty, ai.unit, ai.cost
            FROM activity_input ai
            JOIN input_item ii ON ai.input_id = ii.id
            WHERE ai.activity_id = #{activityId}
            ORDER BY ai.id ASC
            """)
    List<Map<String, Object>> findByActivityId(@Param("activityId") Long activityId);
}
