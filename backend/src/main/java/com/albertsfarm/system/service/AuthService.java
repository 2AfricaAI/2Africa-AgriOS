package com.albertsfarm.system.service;

import com.albertsfarm.common.exception.BusinessException;
import com.albertsfarm.framework.security.JwtUtil;
import com.albertsfarm.system.dto.LoginDTO;
import com.albertsfarm.system.dto.LoginVO;
import com.albertsfarm.system.entity.SysUser;
import com.albertsfarm.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redis;

    @Value("${albertsfarm.jwt.access-token-ttl-minutes:120}")
    private int accessTtlMin;

    private static final String LOGIN_FAIL_KEY = "auth:fail:";
    private static final int FAIL_LIMIT = 5;
    private static final Duration FAIL_TTL = Duration.ofMinutes(15);

    public LoginVO login(LoginDTO dto, String ip) {
        String failKey = LOGIN_FAIL_KEY + dto.getUsername();
        String failStr = redis.opsForValue().get(failKey);
        int failCount = failStr == null ? 0 : Integer.parseInt(failStr);
        if (failCount >= FAIL_LIMIT) {
            throw new BusinessException("登录失败次数过多，账号已锁定 15 分钟");
        }

        SysUser user = userMapper.findByUsername(dto.getUsername());
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            redis.opsForValue().increment(failKey);
            redis.expire(failKey, FAIL_TTL);
            throw new BusinessException("用户名或密码错误");
        }
        if (!"active".equals(user.getStatus())) {
            throw new BusinessException("账号已停用，请联系管理员");
        }
        redis.delete(failKey);

        Set<String> perms = userMapper.findPermsByUserId(user.getId());
        Set<String> roles = userMapper.findRoleCodesByUserId(user.getId());
        String dataScope = userMapper.findMaxDataScope(user.getId());

        Map<String, Object> claims = new HashMap<>();
        claims.put("perms", perms);
        claims.put("roles", roles);
        claims.put("scope", dataScope == null ? "self" : dataScope);

        String access = jwtUtil.issueAccessToken(user.getId(), user.getUsername(), claims);
        String refresh = jwtUtil.issueRefreshToken(user.getId());

        // 更新登录时间和 IP
        userMapper.updateLastLogin(user.getId(), ip);

        log.info("[登录成功] uid={} uname={} ip={} roles={}", user.getId(), user.getUsername(), ip, roles);

        return LoginVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .accessToken(access)
                .refreshToken(refresh)
                .accessTokenExpiresIn(accessTtlMin * 60L)
                .roles(roles)
                .permissions(perms)
                .build();
    }

    /** 登出 - 把 access token 加入 Redis 黑名单直到过期 */
    public void logout(String token) {
        try {
            var claims = jwtUtil.parse(token);
            long ttl = (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000;
            if (ttl > 0) {
                redis.opsForValue().set("jwt:blacklist:" + token, "1", Duration.ofSeconds(ttl));
            }
        } catch (Exception ignored) {}
    }
}
