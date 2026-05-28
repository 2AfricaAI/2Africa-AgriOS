package ai.toafrica.agrios.master.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.master.dto.PackagingSpecForm;
import ai.toafrica.agrios.master.entity.PackagingSpec;
import ai.toafrica.agrios.master.service.PackagingSpecService;
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

@Tag(name = "22 · Master-Packaging Specs", description = "Packaging spec list / detail")
@RestController
@RequestMapping("/v1/master/packaging-specs")
@RequiredArgsConstructor
public class PackagingSpecController {

    private final PackagingSpecService packagingSpecService;

    @Operation(summary = "Packaging spec list (paginated + filtered)")
    @GetMapping
    public R<PageResult<PackagingSpec>> list(
            @Parameter(description = "Name fuzzy match") @RequestParam(required = false) String name,
            @Parameter(description = "Code fuzzy match") @RequestParam(required = false) String code,
            @Parameter(description = "Status: 1=enabled, 0=disabled") @RequestParam(required = false) Integer status,
            PageQuery pq) {
        return R.ok(packagingSpecService.page(name, code, status, pq));
    }

    @Operation(summary = "Packaging spec detail")
    @GetMapping("/{id}")
    public R<PackagingSpec> detail(@PathVariable Long id) {
        return R.ok(packagingSpecService.detail(id));
    }

    @Operation(summary = "Create packaging spec")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping
    public R<Long> create(@Valid @RequestBody PackagingSpecForm form) {
        return R.ok(packagingSpecService.create(form));
    }

    @Operation(summary = "Update packaging spec")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody PackagingSpecForm form) {
        packagingSpecService.update(id, form);
        return R.ok();
    }

    @Operation(summary = "Enable / disable (status: 1=enabled, 0=disabled)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping("/{id}/status/{status}")
    public R<Void> changeStatus(@PathVariable Long id, @PathVariable Integer status) {
        packagingSpecService.changeStatus(id, status);
        return R.ok();
    }
}
