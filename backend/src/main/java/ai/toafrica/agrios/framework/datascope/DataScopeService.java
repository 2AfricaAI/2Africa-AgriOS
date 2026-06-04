package ai.toafrica.agrios.framework.datascope;

import ai.toafrica.agrios.org.service.OrgNodeService;
import ai.toafrica.agrios.org.service.OrgUserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Sprint 51 -- compute and cache the subtree id set for a user.
 *
 * <p>The MyBatis interceptor calls {@link #visibleNodeIds(Long, String)}
 * on every guarded query; we cache the answer in Redis with the TTL
 * configured in {@link DataScopeProperties}. OrgNode mutations call
 * {@link #invalidate(Long)} to drop the cached subtree for the moved /
 * deactivated node.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataScopeService {

    private final OrgUserService orgUserService;
    private final OrgNodeService orgNodeService;
    private final StringRedisTemplate redis;
    private final DataScopeProperties props;
    private final ObjectMapper json = new ObjectMapper();

    private static final TypeReference<List<Long>> LONG_LIST = new TypeReference<>() {};
    private static final String CACHE_PREFIX = "ds:subtree:";

    /**
     * Return the set of node ids the given user can see, for a given
     * {@code data_scope} value. Caller passes the scope rather than
     * having us look it up so we don't N+1 on permission lookups.
     *
     * <ul>
     *   <li>{@code self} -- empty list; caller falls back to created_by filter</li>
     *   <li>{@code group} -- ids of the user's primary node + its subtree</li>
     *   <li>{@code all} -- empty list with the special marker; caller does no filter</li>
     * </ul>
     *
     * @return null if scope is {@code all} (no filter); empty list if user
     *         has no primary node; otherwise the (potentially large)
     *         inclusive subtree id list.
     */
    public List<Long> visibleNodeIds(Long userId, String scope) {
        if (scope == null) return List.of();
        switch (scope.toLowerCase()) {
            case "all":  return null;        // sentinel: no filter
            case "self": return List.of();   // caller uses created_by instead
            case "group":
            default:
                return subtreeFor(userId);
        }
    }

    /** Returns the user's primary node id (cached via OrgUserService). */
    public Long primaryNodeId(Long userId) {
        return orgUserService.currentPrimaryNodeId(userId);
    }

    // -------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------

    private List<Long> subtreeFor(Long userId) {
        Long primary = primaryNodeId(userId);
        if (primary == null) return List.of();
        return cachedSubtree(primary);
    }

    public List<Long> cachedSubtree(Long nodeId) {
        if (nodeId == null) return List.of();
        String key = CACHE_PREFIX + nodeId;
        try {
            String hit = redis.opsForValue().get(key);
            if (hit != null) {
                return json.readValue(hit, LONG_LIST);
            }
        } catch (Exception e) {
            log.warn("[datascope] cache read failed for node={}: {}", nodeId, e.getMessage());
        }

        List<Long> ids = orgNodeService.subtreeIds(nodeId);
        try {
            redis.opsForValue().set(
                    key,
                    json.writeValueAsString(ids),
                    props.getCacheTtlSeconds(),
                    TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("[datascope] cache write failed for node={}: {}", nodeId, e.getMessage());
        }
        return ids;
    }

    /**
     * Drop the subtree cache for a node AND all its ancestors -- if
     * node X moves, the subtree of every ancestor of X is also affected.
     * Lazy: we just blast the whole prefix (cheap for small org).
     */
    public void invalidate(Long nodeId) {
        try {
            var keys = redis.keys(CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) redis.delete(keys);
            log.debug("[datascope] cache invalidated trigger={}", nodeId);
        } catch (Exception e) {
            log.warn("[datascope] cache invalidate failed: {}", e.getMessage());
        }
    }
}
