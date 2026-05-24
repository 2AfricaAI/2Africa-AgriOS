package com.albertsfarm.framework.security;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

/**
 * 登录上下文用户信息 - 存于 SecurityContext
 */
@Data
@AllArgsConstructor
public class LoginUser {
    private Long userId;
    private String username;
    private String dataScope;        // self/group/all
    private Set<String> permissions; // 权限标识集合，例 plot:list
    private Set<String> roleCodes;   // 角色 code 集合
}
