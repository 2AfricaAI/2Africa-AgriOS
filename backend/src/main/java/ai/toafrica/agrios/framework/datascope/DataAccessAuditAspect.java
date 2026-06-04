package ai.toafrica.agrios.framework.datascope;

import ai.toafrica.agrios.framework.security.LoginUser;
import ai.toafrica.agrios.framework.security.SecurityUtil;
import ai.toafrica.agrios.org.entity.DataAccessAudit;
import ai.toafrica.agrios.org.mapper.DataAccessAuditMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Collection;

/**
 * Sprint 51 -- decision #5 falls in here.
 *
 * <p>Every controller method annotated with {@link DataScope} is mirrored
 * by this aspect. After the method returns, IF the calling user's
 * {@code data_scope = 'all'}, we append a row to {@code data_access_audit}
 * recording who looked at what.</p>
 *
 * <p>Write is asynchronous ({@code @Async}) so the API response is not
 * slowed down. Failures are logged but never propagate.</p>
 *
 * <p>Subordinate to {@link DataScopeAspect} (lower priority): runs after
 * the scope context has been pushed and the SQL has executed.</p>
 */
@Slf4j
@Aspect
@Component
@Order(60)
@RequiredArgsConstructor
public class DataAccessAuditAspect {

    private final DataAccessAuditMapper auditMapper;
    private final DataScopeProperties props;

    @AfterReturning(pointcut = "@annotation(ai.toafrica.agrios.framework.datascope.DataScope)",
                    returning = "result")
    public void afterReturning(JoinPoint jp, Object result) {
        if (!props.isAuditEnabled()) return;

        LoginUser user;
        try {
            user = SecurityUtil.current();
        } catch (Exception e) {
            return;
        }
        if (!"all".equalsIgnoreCase(user.getDataScope())) return;

        DataScope ann = ((MethodSignature) jp.getSignature())
                .getMethod().getAnnotation(DataScope.class);
        if (ann == null) return;

        String resource = ann.resource().isBlank() ? ann.table() : ann.resource();
        Integer rowCount = countRows(result);
        String roleCsv = user.getRoleCodes() == null ? null
                : String.join(",", user.getRoleCodes());
        HttpServletRequest req = httpRequest();
        String summary = String.format("%s.%s",
                jp.getSignature().getDeclaringType().getSimpleName(),
                jp.getSignature().getName());

        writeAsync(user.getUserId(), roleCsv, resource, rowCount, summary,
                req != null ? clientIp(req) : null,
                req != null ? req.getHeader("User-Agent") : null);
    }

    @Async
    public void writeAsync(Long userId, String roleCsv, String resource,
                           Integer rowCount, String summary, String ip, String ua) {
        try {
            DataAccessAudit row = new DataAccessAudit();
            row.setUserId(userId);
            row.setRoleCode(roleCsv);
            row.setResourceType(resource);
            row.setQuerySummary(summary);
            row.setRowCount(rowCount);
            row.setIp(ip);
            row.setUserAgent(ua);
            row.setAccessedAt(LocalDateTime.now());
            auditMapper.insert(row);
        } catch (Exception e) {
            log.warn("[datascope-audit] write failed: {}", e.getMessage());
        }
    }

    private static Integer countRows(Object result) {
        if (result == null) return 0;
        // Unwrap common R<T> envelope -- best effort
        try {
            var dataField = result.getClass().getDeclaredField("data");
            dataField.setAccessible(true);
            Object data = dataField.get(result);
            if (data instanceof Collection<?> c) return c.size();
            if (data == null) return 0;
            return 1;
        } catch (Exception ignored) {
            if (result instanceof Collection<?> c) return c.size();
            return 1;
        }
    }

    private static HttpServletRequest httpRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes)
                    RequestContextHolder.getRequestAttributes();
            return attrs == null ? null : attrs.getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    private static String clientIp(HttpServletRequest req) {
        String h = req.getHeader("X-Forwarded-For");
        if (h != null && !h.isBlank()) return h.split(",")[0].trim();
        h = req.getHeader("X-Real-IP");
        if (h != null && !h.isBlank()) return h;
        return req.getRemoteAddr();
    }
}
