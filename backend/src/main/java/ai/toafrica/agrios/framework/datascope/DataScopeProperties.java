package ai.toafrica.agrios.framework.datascope;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Sprint 51 -- knobs for the DataScope subsystem.
 *
 * <pre>
 *   agrios.datascope.enabled=true       # master switch -- Day 5 grey-out point
 *   agrios.datascope.cache-ttl-seconds=3600
 *   agrios.datascope.max-in-list=1000   # above this, use EXISTS subquery
 *   agrios.datascope.audit-enabled=true # decision #5 audit log
 * </pre>
 *
 * <p>Decision #7 -- {@code enabled=false} by default in dev to keep all
 * existing pages working. Production deploy will flip to {@code true}
 * after grey-out validation.</p>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "agrios.datascope")
public class DataScopeProperties {

    /** Master switch. Falls back to a no-op when disabled. */
    private boolean enabled = false;

    /** Redis TTL for the subtree-id cache (seconds). */
    private int cacheTtlSeconds = 3600;

    /** If subtree-id set is larger than this, switch from IN(...) to EXISTS subquery. */
    private int maxInList = 1000;

    /** Whether DataAccessAuditAspect writes rows for 'all'-scope queries. */
    private boolean auditEnabled = true;
}
