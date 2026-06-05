package ai.toafrica.agrios.workflow.service;

import ai.toafrica.agrios.workflow.entity.WfDelegation;
import ai.toafrica.agrios.workflow.mapper.WfDelegationMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Sprint 52 -- resolve "is X currently delegating to Y in module M?".
 *
 * <p>When the engine assigns a step to user X but X has an active
 * delegation in scope, the step is assigned to the delegatee instead
 * (and audit logs the original X as the underlying assignee).</p>
 */
@Service
@RequiredArgsConstructor
public class WorkflowDelegationService {

    private final WfDelegationMapper mapper;

    /**
     * If the user has an active delegation covering {@code module} on
     * the date {@code today}, return the delegatee id; otherwise empty.
     *
     * <p>{@code scope_modules=NULL} means the delegation covers all
     * modules. A CSV like {@code "hr,admin"} covers those two.</p>
     */
    public Optional<Long> resolveDelegatee(Long userId, String module, LocalDate today) {
        if (userId == null) return Optional.empty();
        LocalDate when = today == null ? LocalDate.now() : today;
        var rows = mapper.selectList(new LambdaQueryWrapper<WfDelegation>()
                .eq(WfDelegation::getDelegatorId, userId)
                .eq(WfDelegation::getActive, 1)
                .le(WfDelegation::getFromDate, when)
                .ge(WfDelegation::getToDate, when));
        for (WfDelegation r : rows) {
            String scope = r.getScopeModules();
            if (scope == null || scope.isBlank()) {
                return Optional.ofNullable(r.getDelegateeId());      // all modules
            }
            for (String m : scope.split(",")) {
                if (m.trim().equalsIgnoreCase(module)) {
                    return Optional.ofNullable(r.getDelegateeId());
                }
            }
        }
        return Optional.empty();
    }
}
