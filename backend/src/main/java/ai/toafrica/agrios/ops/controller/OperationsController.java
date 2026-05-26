package ai.toafrica.agrios.ops.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.ops.service.ActionEngineService;
import ai.toafrica.agrios.ops.service.ActionItemService;
import ai.toafrica.agrios.ops.vo.ActionItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Sprint 10 - 经营行动清单 / Operations Action Board.
 *
 * 规则引擎产出的可执行动作 - 每天打开页面就知道该干啥.
 *   GET    /actions             — 列表 (按 category / owner / severity 过滤)
 *   POST   /actions/refresh     — 手动刷新规则引擎 (重算所有 action_item)
 *   POST   /actions/{id}/done   — 用户标记完成
 *   POST   /actions/{id}/dismiss — 用户忽略
 */
@Tag(name = "40 · Operations Action Board",
     description = "Rule-driven action items for daily operations (V2.0 Module 11)")
@RestController
@RequestMapping("/v1/operations/actions")
@RequiredArgsConstructor
public class OperationsController {

    private final ActionItemService actionItemService;
    private final ActionEngineService engineService;

    @Operation(summary = "List action items (paginated, filterable)")
    @GetMapping
    public R<PageResult<ActionItemVO>> list(
            @Parameter(description = "today / week_risk / followup / pause")
                @RequestParam(required = false) String category,
            @Parameter(description = "sales / packhouse / qc / finance / ceo / production")
                @RequestParam(required = false) String ownerRole,
            @Parameter(description = "open / done / dismissed / auto_resolved (default: open)")
                @RequestParam(required = false) String status,
            @Parameter(description = "high / medium / low")
                @RequestParam(required = false) String severity,
            PageQuery pq) {
        return R.ok(actionItemService.page(category, ownerRole, status, severity, pq));
    }

    @Operation(summary = "Manually refresh rule engine — recompute all action items")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or " +
                  "hasAuthority('ROLE_SALES') or hasAuthority('ROLE_PACKHOUSE')")
    @PostMapping("/refresh")
    public R<Map<String, Object>> refresh() {
        int count = engineService.refresh();
        Map<String, Object> body = new HashMap<>();
        body.put("triggered", count);
        return R.ok(body);
    }

    @Operation(summary = "Mark action as done")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or " +
                  "hasAuthority('ROLE_SALES') or hasAuthority('ROLE_PACKHOUSE')")
    @PostMapping("/{id}/done")
    public R<Void> markDone(@PathVariable Long id,
                            @RequestBody(required = false) Map<String, String> body) {
        String remark = body != null ? body.get("remark") : null;
        actionItemService.markDone(id, remark);
        return R.ok();
    }

    @Operation(summary = "Dismiss action (not relevant / false positive)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or " +
                  "hasAuthority('ROLE_SALES') or hasAuthority('ROLE_PACKHOUSE')")
    @PostMapping("/{id}/dismiss")
    public R<Void> dismiss(@PathVariable Long id,
                           @RequestBody(required = false) Map<String, String> body) {
        String remark = body != null ? body.get("remark") : null;
        actionItemService.dismiss(id, remark);
        return R.ok();
    }
}
