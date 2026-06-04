package ai.toafrica.agrios.framework.datascope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sprint 51 -- declares "data scope" filtering for a controller method.
 *
 * <p>When applied, the AOP aspect captures the annotation metadata into
 * a ThreadLocal; the MyBatis inner interceptor reads it and injects a
 * {@code WHERE <column> IN (subtree ids)} (group) or
 * {@code WHERE created_by = ?} (self) clause into the executed SQL.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * @DataScope(table = "plot", column = "node_id")
 * @GetMapping("/v1/plots")
 * public R<List<Plot>> list(PlotQuery q) { ... }
 * }</pre>
 *
 * <p>Decision #5 -- a request from a {@code data_scope='all'} role does
 * NOT have its SQL rewritten, but a row is appended to
 * {@code data_access_audit} by {@code DataAccessAuditAspect}.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScope {

    /**
     * Logical resource name -- used to identify the row in audit logs.
     * Defaults to {@link #table()} when blank.
     */
    String resource() default "";

    /**
     * SQL table the scope applies to. The interceptor only rewrites
     * SELECTs that touch this table; other tables in the same query
     * are untouched.
     */
    String table();

    /**
     * Column on {@link #table()} holding the org_node id. Defaults to
     * {@code node_id}.
     */
    String column() default "node_id";

    /**
     * If true, when scope=self the interceptor uses {@code created_by}
     * instead of {@code node_id} (some tables don't have node_id but
     * have a creator). Falls back to {@code created_by_column}.
     */
    boolean useCreatedByForSelf() default true;

    /** Column for "self" filter. Defaults to {@code created_by}. */
    String createdByColumn() default "created_by";
}
