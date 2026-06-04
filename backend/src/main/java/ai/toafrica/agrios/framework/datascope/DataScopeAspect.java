package ai.toafrica.agrios.framework.datascope;

import ai.toafrica.agrios.framework.security.LoginUser;
import ai.toafrica.agrios.framework.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Sprint 51 -- captures the {@link DataScope} annotation on the calling
 * controller method and pushes its metadata into {@link DataScopeContext}.
 *
 * <p>Runs at a high priority so any other aspects (e.g. permission
 * checks) see a stable thread-local. Pops in {@code finally} to avoid
 * leaking through pooled threads.</p>
 *
 * <p>When {@code agrios.datascope.enabled=false}, the aspect still runs
 * but pushes nothing -- the MyBatis interceptor short-circuits and
 * leaves SQL untouched (decision #7 one-flag rollback).</p>
 */
@Slf4j
@Aspect
@Component
@Order(50)
@RequiredArgsConstructor
public class DataScopeAspect {

    private final DataScopeProperties props;
    private final DataScopeService dsService;

    @Around("@annotation(ai.toafrica.agrios.framework.datascope.DataScope)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        if (!props.isEnabled()) {
            return pjp.proceed();
        }
        DataScope ann = ((org.aspectj.lang.reflect.MethodSignature) pjp.getSignature())
                .getMethod().getAnnotation(DataScope.class);
        if (ann == null) {
            return pjp.proceed();
        }

        LoginUser user;
        try {
            user = SecurityUtil.current();
        } catch (Exception e) {
            // anonymous endpoint -- no scope filtering
            return pjp.proceed();
        }

        Long primaryNode = dsService.primaryNodeId(user.getUserId());
        Set<String> roles = user.getRoleCodes() == null ? Set.of() : user.getRoleCodes();
        String roleCsv = String.join(",", roles);

        DataScopeContext.Holder holder = new DataScopeContext.Holder(
                ann.resource().isBlank() ? ann.table() : ann.resource(),
                ann.table(),
                ann.column(),
                ann.useCreatedByForSelf(),
                ann.createdByColumn(),
                user.getUserId(),
                user.getDataScope(),
                roleCsv,
                primaryNode
        );
        DataScopeContext.push(holder);
        try {
            return pjp.proceed();
        } finally {
            DataScopeContext.pop();
        }
    }
}
