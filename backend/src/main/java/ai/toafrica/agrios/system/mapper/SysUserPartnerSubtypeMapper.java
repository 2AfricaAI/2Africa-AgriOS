package ai.toafrica.agrios.system.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserPartnerSubtypeMapper {

    @Select("SELECT subtype_code FROM sys_user_partner_subtype WHERE user_id = #{userId}")
    List<String> findByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM sys_user_partner_subtype WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    @Insert("INSERT INTO sys_user_partner_subtype (user_id, subtype_code) VALUES (#{userId}, #{code})")
    int insertOne(@Param("userId") Long userId, @Param("code") String code);
}
