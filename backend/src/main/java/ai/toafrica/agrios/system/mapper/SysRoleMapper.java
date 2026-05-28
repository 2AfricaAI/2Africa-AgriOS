package ai.toafrica.agrios.system.mapper;

import ai.toafrica.agrios.system.entity.SysRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /** Find roles attached to a user via sys_user_role. */
    @Select("""
            SELECT r.* FROM sys_role r
            JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
            ORDER BY r.id
            """)
    List<SysRole> findByUserId(@Param("userId") Long userId);

    @Select("SELECT id FROM sys_role WHERE code = #{code}")
    Long findIdByCode(@Param("code") String code);
}
