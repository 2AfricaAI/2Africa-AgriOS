package ai.toafrica.agrios.system.mapper;

import ai.toafrica.agrios.system.entity.SysMenu;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    @Select("SELECT * FROM sys_menu WHERE visible = 1 ORDER BY parent_id, sort, id")
    List<SysMenu> listAll();

    @Select("""
            SELECT menu_id FROM sys_role_menu WHERE role_id = #{roleId}
            """)
    List<Long> findMenuIdsByRoleId(@Param("roleId") Long roleId);

    /** Sprint 36: codes -> ids, for ModulePermMatrix translation. */
    @Select({"""
            <script>
            SELECT id FROM sys_menu
             WHERE code IN
            <foreach collection='codes' item='c' open='(' close=')' separator=','> #{c} </foreach>
            </script>
            """})
    List<Long> findIdsByCodes(@Param("codes") Collection<String> codes);

    /** Sprint 36: button-tier ids whose code starts with any of the given prefixes. */
    @Select({"""
            <script>
            SELECT id FROM sys_menu
             WHERE type = 'button'
               AND (
            <foreach collection='prefixes' item='p' separator=' OR '>
              code LIKE CONCAT(#{p}, '%')
            </foreach>
               )
            </script>
            """})
    List<Long> findButtonIdsByPrefixes(@Param("prefixes") Collection<String> prefixes);

    /** Sprint 36: code of every menu currently bound to a role (used to derive module-level view). */
    @Select("""
            SELECT m.code FROM sys_menu m
              JOIN sys_role_menu rm ON rm.menu_id = m.id
             WHERE rm.role_id = #{roleId}
            """)
    List<String> findCodesByRoleId(@Param("roleId") Long roleId);
}
