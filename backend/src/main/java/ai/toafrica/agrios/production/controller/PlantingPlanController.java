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

@Tag(name = "11 · 生产-种植计划", description = "种植计划: 把地块/作物/品种/时间串起来的核心业务对象")
@RestController
@RequestMapping("/v1/production/planting-plans")
@RequiredArgsConstructor
public class PlantingPlanController {

    private final PlantingPlanService plantingPlanService;

    @Operation(summary = "种植计划列表(分页 + 过滤)")
    @GetMapping
    public R<PageResult<PlantingPlanVO>> list(
            @Parameter(description = "按地块 ID 过滤") @RequestParam(required = false) Long plotId,
            @Parameter(description = "按作物 ID 过滤") @RequestParam(required = false) Long cropId,
            @Parameter(description = "按状态过滤") @RequestParam(required = false) String status,
            @Parameter(description = "编码模糊查询") @RequestParam(required = false) String code,
            PageQuery pq) {
        return R.ok(plantingPlanService.page(plotId, cropId, status, code, pq));
    }

    @Operation(summary = "种植计划详情")
    @GetMapping("/{id}")
    public R<PlantingPlanVO> detail(@PathVariable Long id) {
        return R.ok(plantingPlanService.detail(id));
    }

    @Operation(summary = "新建种植计划")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping
    public R<Long> create(@Valid @RequestBody PlantingPlanForm form) {
        return R.ok(plantingPlanService.create(form));
    }

    @Operation(summary = "修改种植计划")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody PlantingPlanForm form) {
        plantingPlanService.update(id, form);
        return R.ok();
    }

    @Operation(summary = "切换状态 (draft/planned/in_progress/harvested/completed/cancelled)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping("/{id}/status/{status}")
    public R<Void> changeStatus(@PathVariable Long id, @PathVariable String status) {
        plantingPlanService.changeStatus(id, status);
        return R.ok();
    }

    @Operation(summary = "删除 (软删)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        plantingPlanService.delete(id);
        return R.ok();
    }
}
