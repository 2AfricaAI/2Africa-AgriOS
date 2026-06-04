package ai.toafrica.agrios.production.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.framework.datascope.DataScope;
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

@Tag(name = "12 · Production-Activity", description = "Field activity events on a planting plan (sow/fertilize/spray etc.), with photos + approval flow")
@RestController
@RequestMapping("/v1/production/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @Operation(summary = "Activity list (paginated + filtered)")
    @DataScope(table = "activity", resource = "activity")
    @GetMapping
    public R<PageResult<ActivityVO>> list(
            @Parameter(description = "Filter by plot id") @RequestParam(required = false) Long plotId,
            @Parameter(description = "Filter by plan id") @RequestParam(required = false) Long planId,
            @Parameter(description = "Activity type sow/fertilize/spray/weed/water/prune/other")
                @RequestParam(required = false) String activityType,
            @Parameter(description = "occur_date start (inclusive)")
                @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @Parameter(description = "occur_date end (inclusive)")
                @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @Parameter(description = "Review status: pending/approved/rejected")
                @RequestParam(required = false) String auditStatus,
            PageQuery pq) {
        return R.ok(activityService.page(plotId, planId, activityType, dateFrom, dateTo, auditStatus, pq));
    }

    @Operation(summary = "Activity detail")
    @GetMapping("/{id}")
    public R<ActivityVO> detail(@PathVariable Long id) {
        return R.ok(activityService.detail(id));
    }

    @Operation(summary = "Create activity (also from worker mobile)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or " +
                  "hasAuthority('ROLE_LEADER') or hasAuthority('ROLE_WORKER')")
    @PostMapping
    public R<Long> create(@Valid @RequestBody ActivityForm form) {
        return R.ok(activityService.create(form));
    }

    @Operation(summary = "Update activity")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or hasAuthority('ROLE_LEADER')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody ActivityForm form) {
        activityService.update(id, form);
        return R.ok();
    }

    @Operation(summary = "Review (approved / rejected / pending)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping("/{id}/audit")
    public R<Void> audit(@PathVariable Long id, @RequestBody Map<String, String> body) {
        activityService.audit(id, body.get("status"), body.get("remark"));
        return R.ok();
    }

    @Operation(summary = "Delete (use with care - activities are usually kept for audit)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        activityService.delete(id);
        return R.ok();
    }
}