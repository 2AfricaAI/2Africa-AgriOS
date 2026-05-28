package ai.toafrica.agrios.master.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.master.dto.VarietyForm;
import ai.toafrica.agrios.master.entity.Variety;
import ai.toafrica.agrios.master.service.VarietyService;
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

@Tag(name = "21 · Master-Varieties", description = "Varieties list / detail")
@RestController
@RequestMapping("/v1/master/varieties")
@RequiredArgsConstructor
public class VarietyController {

    private final VarietyService varietyService;

    @Operation(summary = "Variety list (paginated + filtered)")
    @GetMapping
    public R<PageResult<Variety>> list(
            @Parameter(description = "Filter by crop id") @RequestParam(required = false) Long cropId,
            @Parameter(description = "Name fuzzy match") @RequestParam(required = false) String name,
            @Parameter(description = "Code fuzzy match") @RequestParam(required = false) String code,
            @Parameter(description = "Status: 1=enabled, 0=disabled") @RequestParam(required = false) Integer status,
            PageQuery pq) {
        return R.ok(varietyService.page(cropId, name, code, status, pq));
    }

    @Operation(summary = "Variety detail")
    @GetMapping("/{id}")
    public R<Variety> detail(@PathVariable Long id) {
        return R.ok(varietyService.detail(id));
    }

    @Operation(summary = "Create variety")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping
    public R<Long> create(@Valid @RequestBody VarietyForm form) {
        return R.ok(varietyService.create(form));
    }

    @Operation(summary = "Update variety")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody VarietyForm form) {
        varietyService.update(id, form);
        return R.ok();
    }

    @Operation(summary = "Enable / disable (status: 1=enabled, 0=disabled)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping("/{id}/status/{status}")
    public R<Void> changeStatus(@PathVariable Long id, @PathVariable Integer status) {
        varietyService.changeStatus(id, status);
        return R.ok();
    }
}
