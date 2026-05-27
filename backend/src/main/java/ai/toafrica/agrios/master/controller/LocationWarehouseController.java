package ai.toafrica.agrios.master.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.master.dto.WarehouseForm;
import ai.toafrica.agrios.master.entity.LocationWarehouse;
import ai.toafrica.agrios.master.service.LocationWarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "23 · 主数据-仓库库位", description = "仓库/库位列表 / 详情")
@RestController
@RequestMapping("/v1/master/warehouses")
@RequiredArgsConstructor
public class LocationWarehouseController {

    private final LocationWarehouseService warehouseService;

    @Operation(summary = "仓库/库位列表(分页 + 过滤)")
    @GetMapping
    public R<PageResult<LocationWarehouse>> list(
            @Parameter(description = "Name fuzzy")  @RequestParam(required = false) String name,
            @Parameter(description = "Code fuzzy")  @RequestParam(required = false) String code,
            @Parameter(description = "Physical type: normal/cold/quarantine")
                @RequestParam(required = false) String type,
            @Parameter(description = "Business purpose (Sprint 22): finished_goods | seed_storage | fertilizer_storage | pesticide_storage | construction_storage | spare_parts_storage | tools_storage | packaging_storage | other_storage")
                @RequestParam(required = false) String purpose,
            @Parameter(description = "Parent id (0=top)") @RequestParam(required = false) Long parentId,
            @Parameter(description = "Status: 1=enabled 0=disabled") @RequestParam(required = false) Integer status,
            PageQuery pq) {
        return R.ok(warehouseService.page(name, code, type, purpose, parentId, status, pq));
    }

    @Operation(summary = "仓库/库位详情")
    @GetMapping("/{id}")
    public R<LocationWarehouse> detail(@PathVariable Long id) {
        return R.ok(warehouseService.detail(id));
    }

    @Operation(summary = "新建仓库/库位")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping
    public R<Long> create(@Valid @RequestBody WarehouseForm form) {
        return R.ok(warehouseService.create(form));
    }

    @Operation(summary = "修改仓库/库位")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody WarehouseForm form) {
        warehouseService.update(id, form);
        return R.ok();
    }

    @Operation(summary = "启用 / 停用 (status: 1=启用 0=停用)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping("/{id}/status/{status}")
    public R<Void> changeStatus(@PathVariable Long id, @PathVariable Integer status) {
        warehouseService.changeStatus(id, status);
        return R.ok();
    }
}
