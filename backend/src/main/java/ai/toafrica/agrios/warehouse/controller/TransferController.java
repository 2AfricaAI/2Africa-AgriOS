package ai.toafrica.agrios.warehouse.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.warehouse.service.TransferService;
import ai.toafrica.agrios.warehouse.vo.TransferDetailVO;
import ai.toafrica.agrios.warehouse.vo.TransferVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "33 · Warehouse Ops-Transfer", description = "Transfer create/confirm/cancel")
@RestController
@RequestMapping("/v1/warehouse/transfer")
@RequiredArgsConstructor
public class TransferController {
    private final TransferService transferService;

    @GetMapping
    public R<PageResult<TransferVO>> list(@RequestParam(required = false) String status, PageQuery pq) {
        return R.ok(transferService.page(status, pq));
    }

    @GetMapping("/{id}")
    public R<TransferDetailVO> detail(@PathVariable Long id) {
        return R.ok(transferService.detail(id));
    }

    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping
    public R<Long> create(@RequestBody CreateTransferRequest req) {
        var items = req.getItems().stream()
                .map(it -> new TransferService.TransferItemInput(it.getInputItemId(), it.getQty())).toList();
        return R.ok(transferService.create(req.getFromWarehouseId(), req.getToWarehouseId(), items, req.getRemark()));
    }

    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping("/{id}/confirm")
    public R<Void> confirm(@PathVariable Long id) { transferService.confirm(id, null); return R.ok(); }

    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id) { transferService.cancel(id); return R.ok(); }

    @Data
    public static class CreateTransferRequest {
        private Long fromWarehouseId;
        private Long toWarehouseId;
        private String remark;
        private List<TransferItemDTO> items;
    }
    @Data
    public static class TransferItemDTO {
        private Long inputItemId;
        private BigDecimal qty;
    }
}
