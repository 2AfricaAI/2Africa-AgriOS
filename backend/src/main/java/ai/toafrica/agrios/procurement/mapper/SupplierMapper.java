package ai.toafrica.agrios.procurement.mapper;

import ai.toafrica.agrios.procurement.entity.Supplier;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SupplierMapper extends BaseMapper<Supplier> {

    /** 取当前最大 code 序号 (用于生成下一个 SUP-NNNNN) */
    @Select("SELECT IFNULL(MAX(CAST(SUBSTRING(code, 5) AS UNSIGNED)), 0) FROM supplier WHERE code LIKE 'SUP-%'")
    int maxCodeSeq();
}
