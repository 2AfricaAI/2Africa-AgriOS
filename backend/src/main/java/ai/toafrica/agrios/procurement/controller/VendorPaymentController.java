package ai.toafrica.agrios.procurement.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.procurement.dto.VendorPaymentForm;
import ai.toafrica.agrios.procurement.entity.VendorPayment;
import ai.toafrica.agrios.procurement.service.AccountsPayableService;
import ai.toafrica.agrios.procurement.service.VendorPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "62 · Procurement-VendorPayment + AP", description = "Vendor payments + Accounts Payable (Sprint 17.5)")
@RestController
@RequestMapping("/v1/procurement")
@RequiredArgsConstructor
public class VendorPaymentController {

    private final VendorPaymentService paymentService;
    private final AccountsPayableService apService;

    @Operation(summary = "List vendor payments")
    @GetMapping("/vendor-payments")
    public R<PageResult<VendorPayment>> list(
            @RequestParam(required = false) Long poId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String method,
            PageQuery pq) {
        return R.ok(paymentService.page(poId, supplierId, method, pq));
    }

    @Operation(summary = "Record a vendor payment")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping("/vendor-payments")
    public R<Long> create(@Valid @RequestBody VendorPaymentForm form) {
        return R.ok(paymentService.create(form));
    }

    @Operation(summary = "Reverse a vendor payment (soft delete)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @DeleteMapping("/vendor-payments/{id}")
    public R<Void> delete(@PathVariable Long id) {
        paymentService.delete(id);
        return R.ok();
    }

    @Operation(summary = "Accounts payable aging by supplier (0-7 / 8-14 / 15-30 / 30+ days)")
    @GetMapping("/ap/aging")
    public R<List<Map<String, Object>>> apAging() {
        return R.ok(apService.apAgingBySupplier());
    }
}
