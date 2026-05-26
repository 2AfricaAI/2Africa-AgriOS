package ai.toafrica.agrios.sales.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.sales.dto.SalesOrderForm;
import ai.toafrica.agrios.sales.service.SalesOrderService;
import ai.toafrica.agrios.sales.vo.SalesOrderDetailVO;
import ai.toafrica.agrios.sales.vo.SalesOrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "31 · Sales-Order", description = "Sales orders + line items")
@RestController
@RequestMapping("/v1/sales/orders")
@RequiredArgsConstructor
public class SalesOrderController {

    private final SalesOrderService orderService;

    @Operation(summary = "List orders (paginated)")
    @GetMapping
    public R<PageResult<SalesOrderVO>> list(
            @RequestParam(required = false) Long customerId,
            @Parameter(description = "Status pending/confirmed/locked/shipping/shipped/delivered/completed/cancelled")
                @RequestParam(required = false) String status,
            @RequestParam(required = false) String code,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            PageQuery pq) {
        return R.ok(orderService.page(customerId, status, code, dateFrom, dateTo, pq));
    }

    @Operation(summary = "Order detail (header + items)")
    @GetMapping("/{id}")
    public R<SalesOrderDetailVO> detail(@PathVariable Long id) {
        return R.ok(orderService.detail(id));
    }

    @Operation(summary = "Create a new order (status=pending)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or hasAuthority('ROLE_SALES')")
    @PostMapping
    public R<Long> create(@Valid @RequestBody SalesOrderForm form) {
        return R.ok(orderService.create(form));
    }

    @Operation(summary = "Edit order (only pending/confirmed)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or hasAuthority('ROLE_SALES')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody SalesOrderForm form) {
        orderService.update(id, form);
        return R.ok();
    }

    @Operation(summary = "Confirm order (pending → confirmed)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or hasAuthority('ROLE_SALES')")
    @PostMapping("/{id}/confirm")
    public R<Void> confirm(@PathVariable Long id) {
        orderService.confirm(id);
        return R.ok();
    }

    @Operation(summary = "Cancel order (pending / confirmed → cancelled)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or hasAuthority('ROLE_SALES')")
    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id) {
        orderService.cancel(id);
        return R.ok();
    }

    @Operation(summary = "Soft delete order (only pending / cancelled)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        orderService.delete(id);
        return R.ok();
    }
}
