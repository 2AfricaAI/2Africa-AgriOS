package ai.toafrica.agrios.org.controller;

import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.org.entity.OrgUser;
import ai.toafrica.agrios.org.service.OrgUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Sprint 51 -- user-to-node membership.
 *
 * <p>Decision #4 -- the time-bounded rows here drive cross-farm payroll
 * attribution. HR Sprint 54-55 will consume these via a SQL view.</p>
 */
@Slf4j
@Tag(name = "81 - ORG - Memberships", description = "User-to-node assignments")
@RestController
@RequestMapping("/v1/org/users")
@RequiredArgsConstructor
public class OrgUserController {

    private final OrgUserService service;

    @Operation(summary = "List a user's active (or full history) memberships")
    @GetMapping("/by-user/{userId}")
    public R<List<OrgUser>> byUser(@PathVariable Long userId,
                                   @RequestParam(defaultValue = "false") boolean history) {
        return R.ok(service.listByUser(userId, history));
    }

    @Operation(summary = "List members of a node (active or all)")
    @GetMapping("/by-node/{nodeId}")
    public R<List<OrgUser>> byNode(@PathVariable Long nodeId,
                                   @RequestParam(defaultValue = "true") boolean activeOnly) {
        return R.ok(service.listByNode(nodeId, activeOnly));
    }

    @Operation(summary = "Current primary node for a user")
    @GetMapping("/by-user/{userId}/primary-node-id")
    public R<Long> currentPrimary(@PathVariable Long userId) {
        return R.ok(service.currentPrimaryNodeId(userId));
    }

    @Operation(summary = "Assign a user to a node "
            + "(closes prior primary if is_primary=1)")
    @PostMapping("/assign")
    public R<OrgUser> assign(@RequestBody OrgUser req) {
        return R.ok(service.assign(req));
    }

    @Operation(summary = "Close a membership (set effective_to)")
    @PostMapping("/{id}/close")
    public R<Void> close(@PathVariable Long id, @RequestBody CloseRequest body) {
        service.close(id, body.getEffectiveTo());
        return R.ok();
    }

    @Data
    public static class CloseRequest {
        private LocalDate effectiveTo;
    }
}
