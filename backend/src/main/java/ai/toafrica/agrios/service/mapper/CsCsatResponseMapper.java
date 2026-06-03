package ai.toafrica.agrios.service.mapper;

import ai.toafrica.agrios.service.entity.CsCsatResponse;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Sprint 50d -- CRUD for {@link CsCsatResponse}. All custom queries go
 * through MyBatis-Plus LambdaQueryWrapper from the service layer; this
 * interface intentionally stays minimal.
 */
@Mapper
public interface CsCsatResponseMapper extends BaseMapper<CsCsatResponse> {
}
