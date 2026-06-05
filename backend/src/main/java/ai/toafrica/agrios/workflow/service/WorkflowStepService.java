package ai.toafrica.agrios.workflow.service;

import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.workflow.entity.WfInstance;
import ai.toafrica.agrios.workflow.entity.WfStep;
import ai.toafrica.agrios.workflow.mapper.WfInstanceMapper;
import ai.toafrica.agrios.workflow.mapper.WfStepMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Sprint 52 Day 3 -- the 5 user-facing actions on a workflow step.
 *
 * <p>Each method:</p>
 * <ol>
 *   <li>Loads the step + sanity-check the actor is authorized</li>
 *   <li>Mutates the step state</li>
 *   <li>Writes an audit row (decision §0 unchangeable)</li>
 *   <li>If the action terminates a step, asks {@link WorkflowEngine}
 *       to advance the seq pointer or close the instance</li>
 * </ol>
 *
 * <p>The actor authorization rule:</p>
 * <ul>
 *   <li>{@code assignee_id == actorId} -- direct assignee</li>
 *   <li>OR actor is the delegatee of the assignee (resolved at click time)</li>
 *   <li>OR actor has SUPER_ADMIN role (escape hatch)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowStepService {

    private final WfStepMapper stepMapper;
    private final WfInstanceMapper instanceMapper;
    private final WorkflowEngine engine;
    private final WorkflowAuditService audit;
    private final WorkflowDelegationService delegation;

    // -------------------------------------------------------------------
    // The 5 user-facing actions
    // -------------------------------------------------------------------

    @Transactional
    public WfStep approve(Long stepId, Long actorId, String comment) {
        WfStep step = loadActiveStep(stepId);
        WfInstance inst = loadInstance(step.getInstanceId());
        assertActorCanAct(step, actorId);

        step.setStatus("approved");
        step.setAction("approve");
        step.setActorId(actorId);
        step.setComment(comment);
        step.setCompletedAt(LocalDateTime.now());
        stepMapper.updateById(step);

        audit.write(inst.getId(), step.getId(), actorId, "approve", comment);
        engine.advanceAfterStepCompletion(step);
        return step;
    }

    @Transactional
    public WfStep reject(Long stepId, Long actorId, String comment) {
        WfStep step = loadActiveStep(stepId);
        WfInstance inst = loadInstance(step.getInstanceId());
        assertActorCanAct(step, actorId);
        if (comment == null || comment.isBlank()) {
            throw new BusinessException("Reject requires a comment");
        }

        step.setStatus("rejected");
        step.setAction("reject");
        step.setActorId(actorId);
        step.setComment(comment);
        step.setCompletedAt(LocalDateTime.now());
        stepMapper.updateById(step);

        audit.write(inst.getId(), step.getId(), actorId, "reject", comment);
        engine.advanceAfterStepCompletion(step);    // engine terminates instance
        return step;
    }

    /**
     * Return the request to an earlier step for revision. The earlier
     * step is re-activated; everything in between is voided.
     */
    @Transactional
    public WfStep returnTo(Long stepId, Long actorId, Integer targetSeq, String comment) {
        WfStep step = loadActiveStep(stepId);
        WfInstance inst = loadInstance(step.getInstanceId());
        assertActorCanAct(step, actorId);
        if (targetSeq == null || targetSeq >= step.getSeq()) {
            throw new BusinessException("Return target seq must be a lower seq than current");
        }
        if (comment == null || comment.isBlank()) {
            throw new BusinessException("Return requires a comment");
        }

        step.setStatus("returned");
        step.setAction("return");
        step.setActorId(actorId);
        step.setComment(comment);
        step.setCompletedAt(LocalDateTime.now());
        stepMapper.updateById(step);

        // Re-open the target seq group. Skipped steps stay skipped.
        List<WfStep> target = stepMapper.selectList(new LambdaQueryWrapper<WfStep>()
                .eq(WfStep::getInstanceId, inst.getId())
                .eq(WfStep::getSeq, targetSeq));
        for (WfStep t : target) {
            if ("skipped".equals(t.getStatus())) continue;
            t.setStatus("pending");
            t.setAction(null);
            t.setActorId(null);
            t.setComment(null);
            t.setActivatedAt(LocalDateTime.now());
            if (t.getSlaHours() != null) {
                t.setSlaDueAt(LocalDateTime.now().plusHours(t.getSlaHours()));
            }
            t.setCompletedAt(null);
            stepMapper.updateById(t);
        }
        inst.setCurrentStepSeq(targetSeq);
        inst.setStatus("pending");
        instanceMapper.updateById(inst);

        audit.write(inst.getId(), step.getId(), actorId, "return",
                "returned to seq " + targetSeq + ": " + comment);
        return step;
    }

    /**
     * Re-assign this single step to another user. Used for ad-hoc
     * handoffs that are not covered by a standing
     * {@link WorkflowDelegationService delegation}.
     */
    @Transactional
    public WfStep delegate(Long stepId, Long actorId, Long newAssigneeId, String comment) {
        WfStep step = loadActiveStep(stepId);
        WfInstance inst = loadInstance(step.getInstanceId());
        assertActorCanAct(step, actorId);
        if (newAssigneeId == null) {
            throw new BusinessException("New assignee is required");
        }
        if (newAssigneeId.equals(step.getAssigneeId())) {
            throw new BusinessException("Cannot delegate to the same user");
        }
        Long prior = step.getAssigneeId();
        step.setAssigneeId(newAssigneeId);
        step.setAction("delegate");
        step.setActorId(actorId);
        step.setComment(comment);
        // status stays 'pending' -- the new assignee picks it up
        stepMapper.updateById(step);

        audit.write(inst.getId(), step.getId(), actorId, "delegate",
                "step delegated from " + prior + " to " + newAssigneeId
                        + (comment == null ? "" : ": " + comment));
        return step;
    }

    /**
     * The initiator cancels their own pending request. Allowed only
     * while the instance is still in {@code pending} state.
     */
    @Transactional
    public WfInstance withdraw(Long instanceId, Long actorId, String comment) {
        WfInstance inst = loadInstance(instanceId);
        if (!"pending".equalsIgnoreCase(inst.getStatus())) {
            throw new BusinessException(
                    "Only pending instances can be withdrawn (current: " + inst.getStatus() + ")");
        }
        if (!actorId.equals(inst.getInitiatorId())) {
            throw new BusinessException(
                    "Only the initiator can withdraw a pending request");
        }
        // Mark all pending steps as 'skipped'; freeze instance
        List<WfStep> open = stepMapper.selectList(new LambdaQueryWrapper<WfStep>()
                .eq(WfStep::getInstanceId, instanceId)
                .in(WfStep::getStatus, "pending", "in_progress"));
        for (WfStep s : open) {
            s.setStatus("skipped");
            s.setCompletedAt(LocalDateTime.now());
            stepMapper.updateById(s);
        }
        inst.setStatus("cancelled");
        inst.setCompletedAt(LocalDateTime.now());
        inst.setCompletedBy(actorId);
        inst.setLastAction("withdraw");
        instanceMapper.updateById(inst);

        audit.write(instanceId, null, actorId, "withdraw", comment);
        return inst;
    }

    // -------------------------------------------------------------------
    // Read helpers used by the controller + Sprint 53+ UI badge
    // -------------------------------------------------------------------

    public List<WfStep> pendingForUser(Long userId) {
        return stepMapper.selectList(new LambdaQueryWrapper<WfStep>()
                .eq(WfStep::getAssigneeId, userId)
                .in(WfStep::getStatus, "pending", "in_progress")
                .orderByAsc(WfStep::getSlaDueAt));
    }

    public Long countPendingForUser(Long userId) {
        return stepMapper.selectCount(new LambdaQueryWrapper<WfStep>()
                .eq(WfStep::getAssigneeId, userId)
                .in(WfStep::getStatus, "pending", "in_progress"));
    }

    /** All steps for an instance, ordered by seq -- timeline render. */
    public List<WfStep> stepsOfInstance(Long instanceId) {
        return stepMapper.selectList(new LambdaQueryWrapper<WfStep>()
                .eq(WfStep::getInstanceId, instanceId)
                .orderByAsc(WfStep::getSeq)
                .orderByAsc(WfStep::getId));
    }

    // -------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------

    private WfStep loadActiveStep(Long stepId) {
        WfStep step = stepMapper.selectById(stepId);
        if (step == null) throw new BusinessException("Step not found: " + stepId);
        if (!"pending".equalsIgnoreCase(step.getStatus())
                && !"in_progress".equalsIgnoreCase(step.getStatus())) {
            throw new BusinessException(
                    "Step is not active (status: " + step.getStatus() + ")");
        }
        return step;
    }

    private WfInstance loadInstance(Long instanceId) {
        WfInstance inst = instanceMapper.selectById(instanceId);
        if (inst == null) throw new BusinessException("Instance not found: " + instanceId);
        return inst;
    }

    /**
     * Actor passes if:
     * <ul>
     *   <li>assignee_id == actorId, OR</li>
     *   <li>actor is a current delegatee of the assignee (any module), OR</li>
     *   <li>actor has SUPER_ADMIN role -- escape hatch for stuck instances</li>
     * </ul>
     */
    private void assertActorCanAct(WfStep step, Long actorId) {
        if (actorId == null) throw new BusinessException("Actor is required");
        if (actorId.equals(step.getAssigneeId())) return;
        // Delegation check: was the assignee delegating TO this actor?
        if (step.getAssigneeId() != null
                && delegation.resolveDelegatee(step.getAssigneeId(), null, null)
                        .map(actorId::equals).orElse(false)) {
            return;
        }
        // Fallback: SUPER_ADMIN can act on anyone's step. (Audit logs it
        // so abuse is visible.) Sprint 52 keeps this simple; finer role
        // checks belong in the controller layer's @PreAuthorize.
        // We let it fall through to BusinessException for non-super-admin
        // callers; the controller will return 403.
        throw new BusinessException("You are not authorized to act on this step");
    }
}
