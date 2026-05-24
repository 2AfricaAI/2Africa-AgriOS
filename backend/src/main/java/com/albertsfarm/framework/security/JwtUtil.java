package com.albertsfarm.framework.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具
 *   - HS512 签名
 *   - access / refresh 双 token
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {

    @Value("${albertsfarm.jwt.secret}")
    private String secret;

    @Value("${albertsfarm.jwt.access-token-ttl-minutes:120}")
    private int accessTtlMin;

    @Value("${albertsfarm.jwt.refresh-token-ttl-days:7}")
    private int refreshTtlDays;

    @Value("${albertsfarm.jwt.issuer:alberts-farm}")
    private String issuer;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String issueAccessToken(Long userId, String username, Map<String, Object> claims) {
        Date now = new Date();
        return Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(userId))
                .claim("uname", username)
                .claim("typ", "access")
                .claims(claims)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTtlMin * 60_000L))
                .signWith(key())
                .compact();
    }

    public String issueRefreshToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(userId))
                .claim("typ", "refresh")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshTtlDays * 86_400_000L))
                .signWith(key())
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key()).build()
                .parseSignedClaims(token).getPayload();
    }

    public boolean isAccessToken(Claims c) {
        return "access".equals(c.get("typ"));
    }
}
