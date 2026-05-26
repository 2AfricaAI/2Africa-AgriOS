package ai.toafrica.agrios.procurement.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.procurement.dto.PurchaseOrderForm;
import ai.toafrica.agrios.procurement.service.PurchaseOrderService;
import ai.toafrica.agrios.procurement.vo.PurchaseOrderDetailVO;
import ai.toafrica.agrios.procurement.vo.PurchaseOrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "61 · Procurement-PurchaseOrder", description = "Purchase orders (Sprint 17)")
@RestController
@RequestMapping("/v1/procurement/orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService service;

    @Operation(summary = "List purchase orders (paginated)")
    @GetMapping
    public R<PageResult<PurchaseOrderVO>> list(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            PageQuery pq) {
        return R.ok(service.page(supplierId, status, code, dateFrom, dateTo, pq));
    }

    @Operation(summary = "PO detail (header + items)")
    @GetMapping("/{id}")
    public R<PurchaseOrderDetailVO> detail(@PathVariable Long id) {
        return R.ok(service.detail(id));
    }

    @Operation(summary = "Create PO (status = draft)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping
    public R<Long> create(@Valid @RequestBody PurchaseOrderForm form) {
        return R.ok(service.create(form));
    }

    @Operation(summary = "Update PO (draft / confirmed only)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody PurchaseOrderForm form) {
        service.update(id, form);
        return R.ok();
    }

    @Operation(summary = "Confirm PO (draft → confirmed)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping("/{id}/confirm")
    public R<Void> confirm(@PathVariable Long id) {
        service.confirm(id);
        return R.ok();
    }

    @Operation(summary = "Mark all items received (confirmed → received)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping("/{id}/receive")
    public R<Void> markReceived(@PathVariable Long id) {
        service.markReceived(id);
        return R.ok();
    }

    @Operation(summary = "Cancel PO (draft / confirmed only)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id) {
        service.cancel(id);
        return R.ok();
    }

    @Operation(summary = "Soft delete PO (draft / cancelled only)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }
}
