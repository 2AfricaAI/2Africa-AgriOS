package ai.toafrica.agrios.finance.mapper;

import ai.toafrica.agrios.finance.entity.SmsTemplate;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SmsTemplateMapper extends BaseMapper<SmsTemplate> {

    @Select("SELECT * FROM sms_template WHERE code = #{code} AND enabled = 1 LIMIT 1")
    SmsTemplate findByCode(@Param("code") String code);
}
