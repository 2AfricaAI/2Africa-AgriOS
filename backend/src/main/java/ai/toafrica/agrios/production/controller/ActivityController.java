package ai.toafrica.agrios.production.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.production.dto.ActivityForm;
import ai.toafrica.agrios.production.service.ActivityService;
import ai.toafrica.agrios.production.vo.ActivityVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@Tag(name = "12 · 生产-农事记录", description = "在种植计划上记一次播种/施肥/打药等农事事件,支持照片 + 审核流")
@RestController
@RequestMapping("/v1/production/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @Operation(summary = "农事记录列表(分页 + 过滤)")
    @GetMapping
    public R<PageResult<ActivityVO>> list(
            @Parameter(description = "按地块 ID 过滤") @RequestParam(required = false) Long plotId,
            @Parameter(description = "按计划 ID 过滤") @RequestParam(required = false) Long planId,
            @Parameter(description = "活动类型 sow/fertilize/spray/weed/water/prune/other")
                @RequestParam(required = false) String activityType,
            @Parameter(description = "occur_date 起 (含)")
                @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @Parameter(description = "occur_date 止 (含)")
                @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @Parameter(description = "审核状态 pending/approved/rejected")
                @RequestParam(required = false) String auditStatus,
            PageQuery pq) {
        return R.ok(activityService.page(plotId, planId, activityType, dateFrom, dateTo, auditStatus, pq));
    }

    @Operation(summary = "农事记录详情")
    @GetMapping("/{id}")
    public R<ActivityVO> detail(@PathVariable Long id) {
        return R.ok(activityService.detail(id));
    }

    @Operation(summary = "新建农事记录 (含工人移动端)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or " +
                  "hasAuthority('ROLE_LEADER') or hasAuthority('ROLE_WORKER')")
    @PostMapping
    public R<Long> create(@Valid @RequestBody ActivityForm form) {
        return R.ok(activityService.create(form));
    }

    @Operation(summary = "修改农事记录")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or hasAuthority('ROLE_LEADER')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody ActivityForm form) {
        activityService.update(id, form);
        return R.ok();
    }

    @Operation(summary = "审核 (approved / rejected / pending)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping("/{id}/audit")
    public R<Void> audit(@PathVariable Long id, @RequestBody Map<String, String> body) {
        activityService.audit(id, body.get("status"), body.get("remark"));
        return R.ok();
    }

    @Operation(summary = "删除 (慎用 - 农事记录通常为审计目的不删)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@Pat