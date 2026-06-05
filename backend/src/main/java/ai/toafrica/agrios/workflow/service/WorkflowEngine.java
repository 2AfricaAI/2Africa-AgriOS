package ai.toafrica.agrios.workflow.service;

import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.org.entity.OrgNode;
import ai.toafrica.agrios.org.entity.OrgUser;
import ai.toafrica.agrios.org.mapper.OrgNodeMapper;
import ai.toafrica.agrios.org.mapper.OrgUserMapper;
import ai.toafrica.agrios.workflow.dsl.WorkflowSchema;
import ai.toafrica.agrios.workflow.dsl.WorkflowSchemaParser;
import ai.toafrica.agrios.workflow.entity.WfDefinition;
import ai.toafrica.agrios.workflow.entity.WfInstance;
import ai.toafrica.agrios.workflow.entity.WfStep;
import ai.toafrica.agrios.workflow.mapper.WfDefinitionMapper;
import ai.toafrica.agrios.workflow.mapper.WfInstanceMapper;
import ai.toafrica.agrios.workflow.mapper.WfStepMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Sprint 52 -- core workflow engine.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Parse {@link WfDefinition#getSchemaJson()} into a {@link WorkflowSchema}</li>
 *   <li>{@link #submit} -- create the {@link WfInstance} + materialize
 *       all steps with their seq groups; activate seq=1 group</li>
 *   <li>{@link #advanceAfterStepCompletion} -- when a step completes,
 *       check if its seq group is fully approved; if so, activate the
 *       next group or close the instance</li>
 *   <li>{@link #resolveAssignee} -- evaluate role / user / lookup
 *       expressions and apply delegation</li>
 *   <li>{@link #evaluateCondition} -- skip a step whose condition fails</li>
 * </ul>
 *
 * <p>State changes here always pair with a {@link WorkflowAuditService}
 * write -- never bypass.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowEngine {

    private final WfDefinitionMapper definitionMapper;
    private final WfInstanceMapper instanceMapper;
    private final WfStepMapper stepMapper;
    private final WorkflowSchemaParser parser;
    private final WorkflowAuditService audit;
    private final WorkflowDelegationService delegation;
    private final OrgUserMapper orgUserMapper;
    private final OrgNodeMapper orgNodeMapper;

    // -------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------

    /**
     * Submit a new workflow instance. Materializes all steps; activates
     * the first non-skipped seq group.
     *
     * @return the freshly created {@link WfInstance}
     */
    @Transactional
    public WfInstance submit(SubmitRequest req) {
        WfDefinition def = loadActiveDefinition(req.getDefinitionCode());
        WorkflowSchema schema = parser.parse(def.getSchemaJson());

        WfInstance inst = new WfInstance();
        inst.setDefinitionId(def.getId());
        inst.setBizTable(req.getBizTable());
        inst.setBizId(req.getBizId());
        inst.setTitle(req.getTitle());
        inst.setStatus("pending");
        inst.setInitiatorId(req.getInitiatorId());
        inst.setAmountHint(req.getAmountHint());
        inst.setUrgency(req.getUrgency() == null ? "normal" : req.getUrgency());
        inst.setNodeId(req.getNodeId());
        inst.setCreatedAt(LocalDateTime.now());
        instanceMapper.insert(inst);

        // Materialize all steps from the schema. Each step's status is
        // 'pending' initially; activate the first seq group only.
        TreeMap<Integer, List<WfStep>> bySeq = new TreeMap<>();
        for (WorkflowSchema.StepSpec spec : schema.getSteps()) {
            if (!evaluateCondition(spec.getCondition(), inst)) {
                // Whole step skipped -- record a placeholder row for audit
                WfStep skipped = newStepFromSpec(inst, def, spec);
                skipped.setStatus("skipped");
                skipped.setCompletedAt(LocalDateTime.now());
                stepMapper.insert(skipped);
                continue;
            }
            WfStep step = newStepFromSpec(inst, def, spec);
            stepMapper.insert(step);
            bySeq.computeIfAbsent(step.getSeq(), k -> new ArrayList<>()).add(step);
        }

        // Activate the lowest seq group that has at least one non-skipped step
        Integer firstSeq = bySeq.isEmpty() ? null : bySeq.firstKey();
        if (firstSeq == null) {
            // All steps skipped -> instance is auto-approved
            instanceComplete(inst, "approved", req.getInitiatorId());
        } else {
            activateSeqGroup(inst, bySeq.get(firstSeq), def.getModule());
            inst.setCurrentStepSeq(firstSeq);
            instanceMapper.updateById(inst);
        }

        audit.write(inst.getId(), null, req.getInitiatorId(),
                "submit", null, null, req.getTitle());
        log.info("[wf] instance#{} submitted def={} biz={}:{}",
                inst.getId(), def.getCode(), inst.getBizTable(), inst.getBizId());
        return inst;
    }

    /**
     * Hook called by step-action handlers after they mark a step as
     * complete (approved / rejected). The engine decides whether to
     * advance the seq pointer or close the instance.
     */
    @Transactional
    public void advanceAfterStepCompletion(WfStep completedStep) {
        WfInstance inst = instanceMapper.selectById(completedStep.getInstanceId());
        if (inst == null) throw new BusinessException("Instance not found");

        // If the step was rejected, the whole instance terminates.
        if ("rejected".equalsIgnoreCase(completedStep.getStatus())) {
            instanceComplete(inst, "rejected", completedStep.getActorId());
            return;
        }

        // Are all steps in the current seq group complete?
        List<WfStep> currentGroup = stepMapper.selectList(new LambdaQueryWrapper<WfStep>()
                .eq(WfStep::getInstanceId, inst.getId())
                .eq(WfStep::getSeq, inst.getCurrentStepSeq()));
        boolean groupComplete = currentGroup.stream().allMatch(s ->
                "approved".equalsIgnoreCase(s.getStatus())
                        || "skipped".equalsIgnoreCase(s.getStatus()));
        if (!groupComplete) return;

        // Find next seq with at least one non-completed step
        List<WfStep> remaining = stepMapper.selectList(new LambdaQueryWrapper<WfStep>()
                .eq(WfStep::getInstanceId, inst.getId())
                .eq(WfStep::getStatus, "pending")
                .gt(WfStep::getSeq, inst.getCurrentStepSeq())
                .orderByAsc(WfStep::getSeq));
        if (remaining.isEmpty()) {
            instanceComplete(inst, "approved", completedStep.getActorId());
            return;
        }
        Integer nextSeq = remaining.get(0).getSeq();
        List<WfStep> nextGroup = new ArrayList<>();
        for (WfStep s : remaining) {
            if (s.getSeq().equals(nextSeq)) nextGroup.add(s);
        }
        WfDefinition def = definitionMapper.selectById(inst.getDefinitionId());
        activateSeqGroup(inst, nextGroup, def == null ? null : def.getModule());
        inst.setCurrentStepSeq(nextSeq);
        instanceMapper.updateById(inst);
    }

    // -------------------------------------------------------------------
    // Helpers used by the engine itself + action handler service
    // -------------------------------------------------------------------

    public WfDefinition loadActiveDefinition(String code) {
        WfDefinition def = definitionMapper.selectOne(new LambdaQueryWrapper<WfDefinition>()
                .eq(WfDefinition::getCode, code)
                .eq(WfDefinition::getActive, 1)
                .orderByDesc(WfDefinition::getVersion)
                .last("LIMIT 1"));
        if (def == null) {
            throw new BusinessException("No active workflow definition with code: " + code);
        }
        return def;
    }

    /**
     * Resolve a step's assignee_spec into a concrete user id. Honors
     * active delegation -- if the resolved user is delegating to X in
     * the relevant module, X gets the task.
     */
    public Long resolveAssignee(WorkflowSchema.AssigneeSpec spec, WfInstance inst, String module) {
        Long base = resolveBase(spec, inst);
        if (base == null) return null;
        return delegation.resolveDelegatee(base, module, LocalDate.now()).orElse(base);
    }

    public boolean evaluateCondition(String condition, WfInstance inst) {
        if (condition == null || condition.isBlank()) return true;
        // Minimal grammar: "amount > N" / "amount >= N" / "amount < N"
        // Sufficient for built-in templates; richer DSL deferred to Sprint 53.
        try {
            String c = condition.trim().toLowerCase().replace(" ", "");
            String op;
            int opLen;
            if      (c.contains(">="))      { op = ">=";  opLen = 2; }
            else if (c.contains("<="))      { op = "<=";  opLen = 2; }
            else if (c.contains(">"))       { op = ">";   opLen = 1; }
            else if (c.contains("<"))       { op = "<";   opLen = 1; }
            else if (c.contains("=="))      { op = "==";  opLen = 2; }
            else return true;
            int idx = c.indexOf(op);
            String lhs = c.substring(0, idx);
            BigDecimal rhs = new BigDecimal(c.substring(idx + opLen));
            BigDecimal v = "amount".equals(lhs) ? inst.getAmountHint() : null;
            if (v == null) return false;
            int cmp = v.compareTo(rhs);
            switch (op) {
                case ">":  return cmp > 0;
                case ">=": return cmp >= 0;
                case "<":  return cmp < 0;
                case "<=": return cmp <= 0;
                case "==": return cmp == 0;
            }
            return false;
        } catch (Exception e) {
            log.warn("[wf] condition '{}' eval failed: {}", condition, e.getMessage());
            return true;        // default-open on parse errors so flow doesn't silently dead-end
        }
    }

    // -------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------

    private WfStep newStepFromSpec(WfInstance inst, WfDefinition def, WorkflowSchema.StepSpec spec) {
        WfStep step = new WfStep();
        step.setInstanceId(inst.getId());
        step.setSeq(spec.getSeq());
        step.setType(spec.getType() == null ? "approval" : spec.getType());
        if (spec.getAssignee() != null) {
            step.setAssigneeId(spec.getAssignee().getUserId());
            step.setAssigneeRole(spec.getAssignee().getRole());
            step.setAssigneeLookup(spec.getAssignee().getLookup());
        }
        step.setStatus("pending");
        step.setSlaHours(spec.getSlaHours());
        return step;
    }

    private void activateSeqGroup(WfInstance inst, List<WfStep> group, String module) {
        LocalDateTime now = LocalDateTime.now();
        for (WfStep s : group) {
            s.setActivatedAt(now);
            if (s.getSlaHours() != null) {
                s.setSlaDueAt(now.plusHours(s.getSlaHours()));
            }
            // Resolve lookup-based assignee now that the step is active
            if (s.getAssigneeId() == null && s.getAssigneeLookup() != null) {
                WorkflowSchema.AssigneeSpec spec = new WorkflowSchema.AssigneeSpec();
                spec.setLookup(s.getAssigneeLookup());
                Long resolved = resolveAssignee(spec, inst, module);
                if (resolved != null) s.setAssigneeId(resolved);
            } else if (s.getAssigneeId() != null) {
                // Direct user assignment -- still honor delegation
                Long resolved = delegation.resolveDelegatee(
                        s.getAssigneeId(), module, LocalDate.now()).orElse(s.getAssigneeId());
                s.setAssigneeId(resolved);
            }
            stepMapper.updateById(s);
        }
    }

    private Long resolveBase(WorkflowSchema.AssigneeSpec spec, WfInstance inst) {
        if (spec == null) return null;
        if (spec.getUserId() != null) return spec.getUserId();
        // Role-based: leave assigneeId null; UI shows "claim" for any role match
        if (spec.getRole() != null && !spec.getRole().isBlank()) return null;
        // Lookup-based -- supported keys (decision: keep small + explicit):
        //   node.manager_id          -- the initiator's primary node manager
        //   node.parent.manager_id   -- parent node manager (one level up)
        //   initiator.id             -- the initiator themselves (rare)
        String key = spec.getLookup();
        if (key == null) return null;
        Long nodeId = inst.getNodeId() != null ? inst.getNodeId()
                : primaryNodeOf(inst.getInitiatorId());
        if (nodeId == null) return null;
        OrgNode node = orgNodeMapper.selectById(nodeId);
        if (node == null) return null;
        switch (key.trim().toLowerCase()) {
            case "node.manager_id":
                return node.getManagerId();
            case "node.parent.manager_id":
                if (node.getParentId() == null) return null;
                OrgNode parent = orgNodeMapper.selectById(node.getParentId());
                return parent == null ? null : parent.getManagerId();
            case "initiator.id":
                return inst.getInitiatorId();
            default:
                log.warn("[wf] unknown assignee lookup '{}'", key);
                return null;
        }
    }

    private Long primaryNodeOf(Long userId) {
        if (userId == null) return null;
        var rows = orgUserMapper.selectList(new LambdaQueryWrapper<OrgUser>()
                .eq(OrgUser::getUserId, userId)
                .eq(OrgUser::getIsPrimary, 1)
                .isNull(OrgUser::getEffectiveTo)
                .last("LIMIT 1"));
        return rows.isEmpty() ? null : rows.get(0).getNodeId();
    }

    private void instanceComplete(WfInstance inst, String finalStatus, Long actorId) {
        inst.setStatus(finalStatus);
        inst.setCompletedAt(LocalDateTime.now());
        inst.setCompletedBy(actorId);
        inst.setLastAction(finalStatus);
        instanceMapper.updateById(inst);
        audit.write(inst.getId(), null, actorId, finalStatus, null, null,
                "instance " + finalStatus);
        log.info("[wf] instance#{} -> {}", inst.getId(), finalStatus);
    }

    // -------------------------------------------------------------------
    // Submit request shape
    // -------------------------------------------------------------------

    @Data
    public static class SubmitRequest {
        private String definitionCode;
        private String bizTable;
        private Long bizId;
        private String title;
        private Long initiatorId;
        private BigDecimal amountHint;
        private String urgency;
        private Long nodeId;
    }
}
