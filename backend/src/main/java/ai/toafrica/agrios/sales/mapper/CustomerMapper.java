package ai.toafrica.agrios.sales.mapper;

import ai.toafrica.agrios.sales.entity.Customer;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {

    /** 取当前最大 code 序号(用于生成下一个 CUS-NNNNN) */
    @Select("SELECT IFNULL(MAX(CAST(SUBSTRING(code, 5) AS UNSIGNED)), 0) FROM customer WHERE code LIKE 'CUS-%'")
    int maxCodeSeq();
}
