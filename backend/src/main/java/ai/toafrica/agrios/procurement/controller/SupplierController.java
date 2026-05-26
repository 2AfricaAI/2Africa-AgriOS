package ai.toafrica.agrios.procurement.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.procurement.dto.SupplierForm;
import ai.toafrica.agrios.procurement.entity.Supplier;
import ai.toafrica.agrios.procurement.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "60 · Procurement-Supplier", description = "Supplier master data (Sprint 17)")
@RestController
@RequestMapping("/v1/procurement/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @Operation(summary = "List suppliers (paginated)")
    @GetMapping
    public R<PageResult<Supplier>> list(
            @Parameter(description = "Fuzzy match name / code / contact / phone")
                @RequestParam(required = false) String keyword,
            @Parameter(description = "Filter by type")
                @RequestParam(required = false) String type,
            @Parameter(description = "Filter by status (active/inactive)")
                @RequestParam(required = false) String status,
            PageQuery pq) {
        return R.ok(supplierService.page(keyword, type, status, pq));
    }

    @Operation(summary = "Supplier detail")
    @GetMapping("/{id}")
    public R<Supplier> detail(@PathVariable Long id) {
        return R.ok(supplierService.detail(id));
    }

    @Operation(summary = "Create supplier")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping
    public R<Long> create(@Valid @RequestBody SupplierForm form) {
        return R.ok(supplierService.create(form));
    }

    @Operation(summary = "Update supplier")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody SupplierForm form) {
        supplierService.update(id, form);
        return R.ok();
    }

    @Operation(summary = "Toggle status (active / inactive)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping("/{id}/status/{status}")
    public R<Void> changeStatus(@PathVariable Long id, @PathVariable String status) {
        supplierService.changeStatus(id, status);
        return R.ok();
    }

    @Operation(summary = "Soft delete supplier")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        supplierService.delete(id);
        return R.ok();
    }
}
