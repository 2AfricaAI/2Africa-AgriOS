package ai.toafrica.agrios.framework.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * JWT 认证过滤器
 *   - 从 Authorization: Bearer xxx 解析 token
 *   - Redis 黑名单校验（支持单点踢出）
 *   - 写入 SecurityContext
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";
    public static final String BLACKLIST_KEY = "jwt:blacklist:";

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redis;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER)) {
            chain.doFilter(req, resp);
            return;
        }
        String token = header.substring(BEARER.length());
        try {
            if (Boolean.TRUE.equals(redis.hasKey(BLACKLIST_KEY + token))) {
                throw new JwtException("Token has been revoked");
            }
            Claims c = jwtUtil.parse(token);
            if (!jwtUtil.isAccessToken(c)) {
                throw new JwtException("Not an access token");
            }
            Long uid = Long.valueOf(c.getSubject());
            String uname = c.get("uname", String.class);
            String dataScope = (String) c.getOrDefault("scope", "self");
            Object permsObj = c.get("perms");
            Set<String> perms = permsObj instanceof List<?> lst
                    ? lst.stream().map(Object::toString).collect(java.util.stream.Collectors.toSet())
                    : Collections.emptySet();
            Object rolesObj = c.get("roles");
            Set<String> roles = rolesObj instanceof List<?> lst
                    ? lst.stream().map(Object::toString).collect(java.util.stream.Collectors.toSet())
                    : Collections.emptySet();

            LoginUser user = new LoginUser(uid, uname, dataScope, perms, roles);
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    user, null,
                    roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList()
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (JwtException ex) {
            log.warn("JWT parse failed: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
        }
        chain.doFilter(req, resp);
    }
}
