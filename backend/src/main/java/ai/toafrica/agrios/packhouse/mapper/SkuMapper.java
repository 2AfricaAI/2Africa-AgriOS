package ai.toafrica.agrios.packhouse.mapper;

import ai.toafrica.agrios.packhouse.entity.Sku;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SkuMapper extends BaseMapper<Sku> {

    /** 按业务维度找已有 SKU (crop+variety+grade+spec 复合) */
    @Select("""
            SELECT * FROM sku
             WHERE crop_id    = #{cropId}
               AND (#{varietyId} IS NULL AND variety_id IS NULL OR variety_id = #{varietyId})
               AND grade      = #{grade}
               AND spec_id    = #{specId}
             LIMIT 1
            """)
    Sku findByDims(@Param("cropId") Long cropId,
                   @Param("varietyId") Long varietyId,
                   @Param("grade") String grade,
                   @Param("specId") Long specId);
}
