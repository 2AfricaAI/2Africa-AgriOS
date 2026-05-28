package ai.toafrica.agrios.system.mapper;

import ai.toafrica.agrios.system.entity.SysUser;
import ai.toafrica.agrios.system.vo.SysUserVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Set;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT * FROM sys_user WHERE username = #{username} AND deleted_at IS NULL")
    SysUser findByUsername(@Param("username") String username);

    /** Paged user list. Roles are loaded separately in the service to keep this simple. */
    @Select("""
            SELECT u.id, u.username, u.nickname, u.phone, u.email, u.avatar,
                   u.status, u.user_type, u.org_name, u.linked_customer_id,
                   u.last_login_at, u.last_login_ip,
                   u.created_at, u.updated_at
              FROM sys_user u
              ${ew.customSqlSegment}
            """)
    IPage<SysUserVO> pageWithJoin(Page<SysUserVO> page,
                                  @Param("ew") QueryWrapper<SysUserVO> wrapper);

    /** 查询用户拥有的权限标识集合（perms 字段） */
    @Select("""
            SELECT DISTINCT m.perms
              FROM sys_user_role ur
              JOIN sys_role_menu rm ON ur.role_id = rm.role_id
              JOIN sys_menu m       ON rm.menu_id = m.id
             WHERE ur.user_id = #{userId}
               AND m.perms IS NOT NULL AND m.perms <> ''
            """)
    Set<String> findPermsByUserId(@Param("userId") Long userId);

    /** 查询用户拥有的角色 code 集合 */
    @Select("""
            SELECT r.code
              FROM sys_user_role ur
              JOIN sys_role r ON ur.role_id = r.id
             WHERE ur.user_id = #{userId}
            """)
    Set<String> findRoleCodesByUserId(@Param("userId") Long userId);

    /** 更新登录时间和登录 IP（不触发 MyBatis-Plus 的 updatedAt 自动填充） */
    @Update("UPDATE sys_user SET last_login_at = NOW(), last_login_ip = #{ip} WHERE id = #{userId}")
    int updateLastLogin(@Param("userId") Long userId, @Param("ip") String ip);

    /** Sprint 35: return every perm string defined in sys_menu — used to grant SUPER_ADMIN full access. */
    @Select("SELECT DISTINCT perms FROM sys_menu WHERE perms IS NOT NULL AND perms <> ''")
    Set<String> findAllMenuPerms();

    /** 查询用户最大的数据范围（all > group > self） */
    @Select("""
            SELECT
              CASE
                WHEN SUM(CASE WHEN r.data_scope = 'all'   THEN 1 ELSE 0 END) > 0 THEN 'all'
                WHEN SUM(CASE WHEN r.data_scope = 'group' THEN 1 ELSE 0 END) > 0 THEN 'group'
                ELSE 'self'
              END
              FROM sys_user_role ur
              JOIN sys_role r ON ur.role_id = r.id
             WHERE ur.user_id = #{userId}
            """)
    String findMaxDataScope(@Param("userId") Long userId);
}
