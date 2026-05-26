package ai.toafrica.agrios.sales.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.sales.dto.CustomerForm;
import ai.toafrica.agrios.sales.entity.Customer;
import ai.toafrica.agrios.sales.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "30 · Sales-Customer", description = "Customer master data")
@RestController
@RequestMapping("/v1/sales/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "List customers (paginated)")
    @GetMapping
    public R<PageResult<Customer>> list(
            @Parameter(description = "Fuzzy match name / code / contact / phone")
                @RequestParam(required = false) String keyword,
            @Parameter(description = "Filter by type")
                @RequestParam(required = false) String type,
            @Parameter(description = "Filter by status (active/inactive)")
                @RequestParam(required = false) String status,
            PageQuery pq) {
        return R.ok(customerService.page(keyword, type, status, pq));
    }

    @Operation(summary = "Customer detail")
    @GetMapping("/{id}")
    public R<Customer> detail(@PathVariable Long id) {
        return R.ok(customerService.detail(id));
    }

    @Operation(summary = "Create customer")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or hasAuthority('ROLE_SALES')")
    @PostMapping
    public R<Long> create(@Valid @RequestBody CustomerForm form) {
        return R.ok(customerService.create(form));
    }

    @Operation(summary = "Update customer")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or hasAuthority('ROLE_SALES')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody CustomerForm form) {
        customerService.update(id, form);
        return R.ok();
    }

    @Operation(summary = "Toggle status (active / inactive)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or hasAuthority('ROLE_SALES')")
    @PostMapping("/{id}/status/{status}")
    public R<Void> changeStatus(@PathVariable Long id, @PathVariable String status) {
        customerService.changeStatus(id, status);
        return R.ok();
    }

    @Operation(summary = "Soft delete customer")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return R.ok();
    }
}
