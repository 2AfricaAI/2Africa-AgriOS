package ai.toafrica.agrios.sales.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.sales.dto.ShipForm;
import ai.toafrica.agrios.sales.service.OutboundService;
import ai.toafrica.agrios.sales.service.PickingService;
import ai.toafrica.agrios.sales.vo.FulfillmentDetailVO;
import ai.toafrica.agrios.sales.vo.FulfillmentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "32 · Sales-Fulfillment", description = "Picking + Outbound (FEFO lock + ship + deliver)")
@RestController
@RequestMapping("/v1/sales/fulfillments")
@RequiredArgsConstructor
public class PickingController {

    private final PickingService pickingService;
    private final OutboundService outboundService;

    @Operation(summary = "List fulfillments (paginated)")
    @GetMapping
    public R<PageResult<FulfillmentVO>> list(
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Long customerId,
            @Parameter(description = "pending/picking/ready/shipped/delivered/cancelled")
                @RequestParam(required = false) String status,
            @RequestParam(required = false) String code,
            PageQuery pq) {
        return R.ok(pickingService.page(orderId, customerId, status, code, pq));
    }

    @Operation(summary = "Fulfillment detail (header + items)")
    @GetMapping("/{id}")
    public R<FulfillmentDetailVO> detail(@PathVariable Long id) {
        return R.ok(pickingService.detail(id));
    }

    @Operation(summary = "List all fulfillments for an order")
    @GetMapping("/by-order/{orderId}")
    public R<List<FulfillmentVO>> listByOrder(@PathVariable Long orderId) {
        return R.ok(pickingService.listByOrder(orderId));
    }

    @Operation(summary = "Pick an order - FEFO lock inventory (earliest-expiry first)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or hasAuthority('ROLE_PACKHOUSE') or hasAuthority('ROLE_SALES')")
    @PostMapping("/pick/{orderId}")
    public R<Long> pick(@PathVariable Long orderId) {
        return R.ok(pickingService.pick(orderId));
    }

    @Operation(summary = "Cancel picking - release locks, fulfillment -> cancelled")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or hasAuthority('ROLE_PACKHOUSE')")
    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id) {
        pickingService.cancel(id);
        return R.ok();
    }

    @Operation(summary = "Ship - fulfillment 'ready' -> 'shipped', deduct qty_locked, generate Revenue rows")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or hasAuthority('ROLE_PACKHOUSE') or hasAuthority('ROLE_SALES')")
    @PostMapping("/{id}/ship")
    public R<Void> ship(@PathVariable Long id, @Valid @RequestBody(required = false) ShipForm form) {
        outboundService.ship(id, form);
        return R.ok();
    }

    @Operation(summary = "Mark delivered (customer signed) - 'shipped' -> 'delivered'")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or hasAuthority('ROLE_SALES')")
    @PostMapping("/{id}/deliver")
    public R<Void> deliver(@PathVariable Long id) {
        outboundService.deliver(id);
        return R.ok();
    }
}
