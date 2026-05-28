package ai.toafrica.agrios.production.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.production.dto.PlantingPlanForm;
import ai.toafrica.agrios.production.service.PlantingPlanService;
import ai.toafrica.agrios.production.vo.PlantingPlanVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

@Tag(name = "11 · Production-Planting Plans", description = "Planting plan: core business object linking plot/crop/variety/time")
@RestController
@RequestMapping("/v1/production/planting-plans")
@RequiredArgsConstructor
public class PlantingPlanController {

    private final PlantingPlanService plantingPlanService;

    @Operation(summary = "Planting plan list (paginated + filtered)")
    @GetMapping
    public R<PageResult<PlantingPlanVO>> list(
            @Parameter(description = "Filter by plot id") @RequestParam(required = false) Long plotId,
            @Parameter(description = "Filter by crop id") @RequestParam(required = false) Long cropId,
            @Parameter(description = "Filter by status") @RequestParam(required = false) String status,
            @Parameter(description = "Code fuzzy match") @RequestParam(required = false) String code,
            PageQuery pq) {
        return R.ok(plantingPlanService.page(plotId, cropId, status, code, pq));
    }

    @Operation(summary = "Planting plan detail")
    @GetMapping("/{id}")
    public R<PlantingPlanVO> detail(@PathVariable Long id) {
        return R.ok(plantingPlanService.detail(id));
    }

    @Operation(summary = "Create planting plan")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping
    public R<Long> create(@Valid @RequestBody PlantingPlanForm form) {
        return R.ok(plantingPlanService.create(form));
    }

    @Operation(summary = "Update planting plan")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody PlantingPlanForm form) {
        plantingPlanService.update(id, form);
        return R.ok();
    }

    @Operation(summary = "Change status (draft/planned/in_progress/harvested/completed/cancelled)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping("/{id}/status/{status}")
    public R<Void> changeStatus(@PathVariable Long id, @PathVariable String status) {
        plantingPlanService.changeStatus(id, status);
        return R.ok();
    }

    @Operation(summary = "Delete (soft delete)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        plantingPlanService.delete(id);
        return R.ok();
    }
}
