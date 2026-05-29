package ai.toafrica.agrios.sales.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.framework.importer.ImportResult;
import ai.toafrica.agrios.framework.importer.ImportRunner;
import ai.toafrica.agrios.sales.dto.CustomerForm;
import ai.toafrica.agrios.sales.entity.Customer;
import ai.toafrica.agrios.sales.service.CustomerService;
import ai.toafrica.agrios.sales.service.importer.CustomerImportTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "30 · Sales-Customer", description = "Customer master data")
@RestController
@RequestMapping("/v1/sales/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final ImportRunner importRunner;
    private final CustomerImportTemplate customerImportTemplate;

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

    // ============================================================
    // Sprint 38f - Excel bulk import
    // ============================================================

    @Operation(summary = "Download a blank import template (xlsx)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or hasAuthority('ROLE_SALES')")
    @GetMapping("/import/template")
    public ResponseEntity<ByteArrayResource> importTemplate() {
        byte[] bytes = importRunner.buildTemplate(customerImportTemplate);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=customers_import_template.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new ByteArrayResource(bytes));
    }

    @Operation(summary = "Bulk import customers from xlsx")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or hasAuthority('ROLE_SALES')")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<ImportResult> importCustomers(@RequestPart("file") MultipartFile file) {
        return R.ok(importRunner.run(customerImportTemplate, file));
    }
}
