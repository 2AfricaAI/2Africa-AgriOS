package ai.toafrica.agrios.system.service;

import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.framework.security.JwtUtil;
import ai.toafrica.agrios.system.dto.LoginDTO;
import ai.toafrica.agrios.system.dto.LoginVO;
import ai.toafrica.agrios.system.entity.SysUser;
import ai.toafrica.agrios.system.mapper.SysUserMapper;
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

    @Value("${agrios.jwt.access-token-ttl-minutes:120}")
    private int accessTtlMin;

    private static final String LOGIN_FAIL_KEY = "auth:fail:";
    private static final int FAIL_LIMIT = 5;
    private static final Duration FAIL_TTL = Duration.ofMinutes(15);

    public LoginVO login(LoginDTO dto, String ip) {
        String failKey = LOGIN_FAIL_KEY + dto.getUsername();
        String failStr = redis.opsForValue().get(failKey);
        int failCount = failStr == null ? 0 : Integer.parseInt(failStr);
        if (failCount >= FAIL_LIMIT) {
            throw new BusinessException("Too many failed login attempts, account locked for 15 minutes");
        }

        SysUser user = userMapper.findByUsername(dto.getUsername());
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            redis.opsForValue().increment(failKey);
            redis.expire(failKey, FAIL_TTL);
            throw new BusinessException("Invalid username or password");
        }
        if (!"active".equals(user.getStatus())) {
            throw new BusinessException("Account is disabled, please contact administrator");
        }
        redis.delete(failKey);

        Set<String> roles = userMapper.findRoleCodesByUserId(user.getId());
        // Hotfix v3.4.1: perms are NO LONGER baked into the JWT. They live in
        // Redis (auth:perms:<uid>) and are loaded by JwtAuthFilter on each
        // request. We still compute them once here so the LoginVO (sent back
        // to the browser for menu rendering) carries them, and so the Redis
        // cache is warm immediately after login.
        Set<String> perms = roles.contains("SUPER_ADMIN")
                ? userMapper.findAllMenuPerms()
                : userMapper.findPermsByUserId(user.getId());
        String dataScope = userMapper.findMaxDataScope(user.getId());

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);
        claims.put("scope", dataScope == null ? "self" : dataScope);
        // Sprint 37: stash user-type + linked customer in the JWT so SecurityUtil
        // and CUSTOMER scope filters can read them without hitting the DB.
        claims.put("utype", user.getUserType() == null ? "STAFF" : user.getUserType());
        if (user.getLinkedCustomerId() != null) {
            claims.put("lcid", user.getLinkedCustomerId());
        }

        String access = jwtUtil.issueAccessToken(user.getId(), user.getUsername(), claims);
        String refresh = jwtUtil.issueRefreshToken(user.getId());

        // 更新登录时间和 IP
        userMapper.updateLastLogin(user.getId(), ip);

        log.info("[Login OK] uid={} uname={} ip={} type={} roles={}",
                user.getId(), user.getUsername(), ip, user.getUserType(), roles);

        // Sprint 37: pick landing path by user_type. WORKER continues to use the
        // existing isWorkerOnly check on the frontend; here we only branch on
        // CUSTOMER / PARTNER vs default desktop.
        String userType = user.getUserType() == null ? "STAFF" : user.getUserType();
        String landing = switch (userType) {
            case "CUSTOMER" -> "/portal/orders";
            default -> "/";
        };

        return LoginVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .accessToken(access)
                .refreshToken(refresh)
                .accessTokenExpiresIn(accessTtlMin * 60L)
                .roles(roles)
                .permissions(perms)
                .userType(userType)
                .landingPath(landing)
                .linkedCustomerId(user.getLinkedCustomerId())
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
