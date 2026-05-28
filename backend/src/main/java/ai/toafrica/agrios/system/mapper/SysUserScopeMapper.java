package ai.toafrica.agrios.system.mapper;

import ai.toafrica.agrios.system.entity.SysUserScope;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserScopeMapper extends BaseMapper<SysUserScope> {

    @Select("SELECT * FROM sys_user_scope WHERE user_id = #{userId} ORDER BY scope_type, id")
    List<SysUserScope> findByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM sys_user_scope WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    /** Return scope_id list for one user, one scope_type, AS OF today. */
    @Select("""
            SELECT scope_id FROM sys_user_scope
             WHERE user_id = #{userId} AND scope_type = #{scopeType}
               AND (valid_from IS NULL OR valid_from <= CURRENT_DATE)
               AND (valid_to   IS NULL OR valid_to   >= CURRENT_DATE)
            """)
    List<Long> findActiveScopeIds(@Param("userId") Long userId,
                                  @Param("scopeType") String scopeType);
}
