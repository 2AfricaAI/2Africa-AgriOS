package ai.toafrica.agrios.master.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.master.dto.CropForm;
import ai.toafrica.agrios.master.entity.Crop;
import ai.toafrica.agrios.master.service.CropService;
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

@Tag(name = "20 · Master-Crops", description = "Crops list / detail")
@RestController
@RequestMapping("/v1/master/crops")
@RequiredArgsConstructor
public class CropController {

    private final CropService cropService;

    @Operation(summary = "Crop list (paginated + filtered)")
    @GetMapping
    public R<PageResult<Crop>> list(
            @Parameter(description = "Name fuzzy match") @RequestParam(required = false) String name,
            @Parameter(description = "Code fuzzy match") @RequestParam(required = false) String code,
            @Parameter(description = "Category (leafy / fruit / root ...)") @RequestParam(required = false) String category,
            @Parameter(description = "Status: 1=enabled, 0=disabled") @RequestParam(required = false) Integer status,
            PageQuery pq) {
        return R.ok(cropService.page(name, code, category, status, pq));
    }

    @Operation(summary = "Crop detail")
    @GetMapping("/{id}")
    public R<Crop> detail(@PathVariable Long id) {
        return R.ok(cropService.detail(id));
    }

    @Operation(summary = "Create crop")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping
    public R<Long> create(@Valid @RequestBody CropForm form) {
        return R.ok(cropService.create(form));
    }

    @Operation(summary = "Update crop")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody CropForm form) {
        cropService.update(id, form);
        return R.ok();
    }

    @Operation(summary = "Enable / disable (status: 1=enabled, 0=disabled)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping("/{id}/status/{status}")
    public R<Void> changeStatus(@PathVariable Long id, @PathVariable Integer status) {
        cropService.changeStatus(id, status);
        return R.ok();
    }
}
