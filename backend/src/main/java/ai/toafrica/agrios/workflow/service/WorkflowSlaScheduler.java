package ai.toafrica.agrios.workflow.service;

import ai.toafrica.agrios.org.entity.OrgNode;
import ai.toafrica.agrios.org.mapper.OrgNodeMapper;
import ai.toafrica.agrios.workflow.entity.WfInstance;
import ai.toafrica.agrios.workflow.entity.WfStep;
import ai.toafrica.agrios.workflow.mapper.WfInstanceMapper;
import ai.toafrica.agrios.workflow.mapper.WfStepMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Sprint 52 Day 4 -- SLA + escalation watchdog.
 *
 * <p>Every 5 minutes, scans for pending steps whose {@code sla_due_at}
 * is in the past. For each:</p>
 * <ol>
 *   <li>Mark the step's {@code escalated_to_id} = the assignee's
 *       manager (walks {@code org_node.ancestors} 1 level up)</li>
 *   <li>Audit the escalation</li>
 *   <li>The original assignee can still act -- escalation does not
 *       reassign, it just adds visibility for the upper level</li>
 * </ol>
 *
 * <p>Sprint 53+ HR sprint will hook this scheduler to Chatwoot / SMS
 * outbound notifications. For now the escalation is silent (only
 * visible in audit log + dashboard).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowSlaScheduler {

    private final WfStepMapper stepMapper;
    private final WfInstanceMapper instanceMapper;
    private final OrgNodeMapper orgNodeMapper;
    private final WorkflowAuditService audit;

    /** Cron: every 5 minutes. Override with {@code agrios.wf.sla-scan-cron}. */
    @Scheduled(cron = "${agrios.wf.sla-scan-cron:0 */5 * * * *}")
    public void scanSlaBreaches() {
        LocalDateTime now = LocalDateTime.now();
        List<WfStep> breaches = stepMapper.selectList(new LambdaQueryWrapper<WfStep>()
                .in(WfStep::getStatus, "pending", "in_progress")
                .isNotNull(WfStep::getSlaDueAt)
                .le(WfStep::getSlaDueAt, now)
                .isNull(WfStep::getEscalatedToId));
        if (breaches.isEmpty()) return;
        log.info("[wf-sla] found {} SLA-breached steps", breaches.size());
        for (WfStep s : breaches) {
            try { escalate(s); }
            catch (Exception e) {
                log.warn("[wf-sla] escalate failed step={}: {}", s.getId(), e.getMessage());
            }
        }
    }

    @Transactional
    public void escalate(WfStep step) {
        Long parentManager = findParentManagerOf(step.getAssigneeId());
        if (parentManager == null) {
            log.debug("[wf-sla] no parent manager for assignee={} step={}",
                    step.getAssigneeId(), step.getId());
            return;
        }
        step.setEscalatedToId(parentManager);
        stepMapper.updateById(step);

        WfInstance inst = instanceMapper.selectById(step.getInstanceId());
        Long instId = inst == null ? null : inst.getId();
        audit.write(instId, step.getId(), 0L, "escalate",
                "SLA breach -- escalated to user#" + parentManager);
        log.info("[wf-sla] step={} SLA breached, escalated to user#{}",
                step.getId(), parentManager);
    }

    /**
     * Walks the org tree to find the user one level above {@code userId}.
     * Strategy: find the user's primary node's parent node's manager.
     * Returns null if anywhere along the path is null.
     */
    private Long findParentManagerOf(Long userId) {
        if (userId == null) return null;
        // We don't have a direct user -> node mapping cached anywhere
        // performance-critical for SLA scan; fall back to org_node where
        // manager_id = userId, then walk up one level. This is O(tree)
        // per scan, but the tree is < 50 nodes for years -- fine.
        List<OrgNode> nodesWhereUserIsManager = orgNodeMapper.selectList(
                new LambdaQueryWrapper<OrgNode>()
                        .eq(OrgNode::getManagerId, userId)
                        .isNull(OrgNode::getDeletedAt)
                        .last("LIMIT 1"));
        if (nodesWhereUserIsManager.isEmpty()) return null;
        OrgNode node = nodesWhereUserIsManager.get(0);
        if (node.getParentId() == null) return null;
        OrgNode parent = orgNodeMapper.selectById(node.getParentId());
        return parent == null ? null : parent.getManagerId();
    }
}
