package ai.toafrica.agrios.framework.security;

import ai.toafrica.agrios.system.mapper.SysUserMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Hotfix v3.4.1 -- moves the perm string list OUT of JWT and into Redis.
 *
 * <p>Why: pre-hotfix tokens carried 100+ perm strings, bloating JWT to
 * ~3KB. That risked nginx / cloud LB header overflows and slowed every
 * request. Roles + scope stay in the JWT (small); perms are resolved on
 * each request from Redis cache (DB miss-fill).</p>
 *
 * <p>Cache key: {@code auth:perms:<uid>}. TTL 1h. Invalidated by
 * {@link #invalidate(Long)} when the user's roles or menu bindings
 * change.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionCacheService {

    private final SysUserMapper userMapper;
    private final StringRedisTemplate redis;
    private final ObjectMapper json = new ObjectMapper();

    private static final TypeReference<Set<String>> SET_OF_STRING = new TypeReference<>() {};
    private static final String KEY_PREFIX = "auth:perms:";
    private static final long TTL_SECONDS = 3600;     // 1h

    /**
     * Returns the user's effective perm set. Uses Redis as a cache.
     * SUPER_ADMIN gets every menu perm; other users get
     * {@code findPermsByUserId(uid)}.
     */
    public Set<String> permsFor(Long uid, Set<String> roles) {
        if (uid == null) return Set.of();
        String key = KEY_PREFIX + uid;
        try {
            String hit = redis.opsForValue().get(key);
            if (hit != null) return json.readValue(hit, SET_OF_STRING);
        } catch (Exception e) {
            log.warn("[perm-cache] read failed uid={}: {}", uid, e.getMessage());
        }
        Set<String> perms = (roles != null && roles.contains("SUPER_ADMIN"))
                ? userMapper.findAllMenuPerms()
                : userMapper.findPermsByUserId(uid);
        try {
            redis.opsForValue().set(key, json.writeValueAsString(perms),
                    TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("[perm-cache] write failed uid={}: {}", uid, e.getMessage());
        }
        return perms;
    }

    /** Clear cache for a single user. Call after role / menu changes. */
    public void invalidate(Long uid) {
        if (uid == null) return;
        try { redis.delete(KEY_PREFIX + uid); }
        catch (Exception e) { log.warn("[perm-cache] invalidate failed: {}", e.getMessage()); }
    }

    /** Clear cache for everybody. Call after a sys_role_menu mass change. */
    public void invalidateAll() {
        try {
            var keys = redis.keys(KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) redis.delete(keys);
        } catch (Exception e) {
            log.warn("[perm-cache] invalidateAll failed: {}", e.getMessage());
        }
    }
}
