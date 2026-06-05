package ai.toafrica.agrios.workflow.controller;

import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.framework.security.SecurityUtil;
import ai.toafrica.agrios.workflow.entity.WfInstance;
import ai.toafrica.agrios.workflow.entity.WfStep;
import ai.toafrica.agrios.workflow.service.WorkflowEngine;
import ai.toafrica.agrios.workflow.service.WorkflowStepService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Sprint 52 Day 4 -- REST surface for the workflow engine.
 *
 * <p>Embedded UI rule (PRD-WORKFLOW § 2.3): there is no central "inbox"
 * page. Each business module renders a badge using
 * {@link #countPending} and a list using {@link #pendingForMe}; the
 * details and the approve/reject buttons are inlined into the
 * business record's own page.</p>
 */
@Tag(name = "85 - WF - Engine", description = "Workflow approvals")
@RestController
@RequestMapping("/v1/wf")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowEngine engine;
    private final WorkflowStepService stepService;

    // -------------------------------------------------------------------
    // Submit / read
    // -------------------------------------------------------------------

    @Operation(summary = "Submit a new workflow instance")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/submit")
    public R<WfInstance> submit(@RequestBody WorkflowEngine.SubmitRequest req) {
        if (req.getInitiatorId() == null) {
            req.setInitiatorId(SecurityUtil.currentUserId());
        }
        return R.ok(engine.submit(req));
    }

    @Operation(summary = "My pending steps (used by module badges)")
    @PreAuthorize("hasAuthority('wf:instance:list')")
    @GetMapping("/me/pending")
    public R<List<WfStep>> pendingForMe() {
        return R.ok(stepService.pendingForUser(SecurityUtil.currentUserId()));
    }

    @Operation(summary = "Count of my pending steps (badge number)")
    @PreAuthorize("hasAuthority('wf:instance:list')")
    @GetMapping("/me/pending/count")
    public R<Map<String, Long>> countPending() {
        return R.ok(Map.of(
                "count", stepService.countPendingForUser(SecurityUtil.currentUserId())
        ));
    }

    @Operation(summary = "Timeline of an instance (for the detail page)")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/instances/{id}/steps")
    public R<List<WfStep>> stepsOfInstance(@PathVariable Long id) {
        return R.ok(stepService.stepsOfInstance(id));
    }

    // -------------------------------------------------------------------
    // The 5 actions
    // -------------------------------------------------------------------

    @Operation(summary = "Approve a step")
    @PreAuthorize("hasAuthority('wf:instance:approve')")
    @PostMapping("/steps/{stepId}/approve")
    public R<WfStep> approve(@PathVariable Long stepId, @RequestBody(required = false) ActionBody body) {
        return R.ok(stepService.approve(stepId, SecurityUtil.currentUserId(),
                body == null ? null : body.getComment()));
    }

    @Operation(summary = "Reject a step (comment required)")
    @PreAuthorize("hasAuthority('wf:instance:approve')")
    @PostMapping("/steps/{stepId}/reject")
    public R<WfStep> reject(@PathVariable Long stepId, @RequestBody ActionBody body) {
        return R.ok(stepService.reject(stepId, SecurityUtil.currentUserId(), body.getComment()));
    }

    @Operation(summary = "Return to an earlier seq for revision")
    @PreAuthorize("hasAuthority('wf:instance:approve')")
    @PostMapping("/steps/{stepId}/return")
    public R<WfStep> returnTo(@PathVariable Long stepId, @RequestBody ReturnBody body) {
        return R.ok(stepService.returnTo(stepId, SecurityUtil.currentUserId(),
                body.getTargetSeq(), body.getComment()));
    }

    @Operation(summary = "Delegate this single step to another user")
    @PreAuthorize("hasAuthority('wf:instance:delegate')")
    @PostMapping("/steps/{stepId}/delegate")
    public R<WfStep> delegate(@PathVariable Long stepId, @RequestBody DelegateBody body) {
        return R.ok(stepService.delegate(stepId, SecurityUtil.currentUserId(),
                body.getNewAssigneeId(), body.getComment()));
    }

    @Operation(summary = "Withdraw a pending instance (initiator only)")
    @PreAuthorize("hasAuthority('wf:instance:withdraw')")
    @PostMapping("/instances/{instanceId}/withdraw")
    public R<WfInstance> withdraw(@PathVariable Long instanceId,
                                  @RequestBody(required = false) ActionBody body) {
        return R.ok(stepService.withdraw(instanceId, SecurityUtil.currentUserId(),
                body == null ? null : body.getComment()));
    }

    // -------------------------------------------------------------------
    // Request bodies
    // -------------------------------------------------------------------

    @Data
    public static class ActionBody {
        private String comment;
    }
    @Data
    public static class ReturnBody {
        private Integer targetSeq;
        private String comment;
    }
    @Data
    public static class DelegateBody {
        private Long newAssigneeId;
        private String comment;
    }
}
