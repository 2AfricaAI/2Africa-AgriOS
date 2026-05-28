package ai.toafrica.agrios.warehouse.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.warehouse.service.StocktakeService;
import ai.toafrica.agrios.warehouse.vo.StocktakeDetailVO;
import ai.toafrica.agrios.warehouse.vo.StocktakeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "32 · Warehouse Ops-Stocktake", description = "Stocktake create/count/confirm/cancel")
@RestController
@RequestMapping("/v1/warehouse/stocktake")
@RequiredArgsConstructor
public class StocktakeController {

    private final StocktakeService stocktakeService;

    @GetMapping
    public R<PageResult<StocktakeVO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String countType,
            PageQuery pq) {
        return R.ok(stocktakeService.page(status, warehouseId, countType, pq));
    }

    @GetMapping("/{id}")
    public R<StocktakeDetailVO> detail(@PathVariable Long id) {
        return R.ok(stocktakeService.detail(id));
    }

    @Operation(summary = "Create stocktake (auto-snapshot system qty)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping
    public R<Long> create(@RequestBody CreateRequest req) {
        return R.ok(stocktakeService.create(req.getWarehouseId(), req.getCountType(), req.getRemark()));
    }

    @Operation(summary = "Submit count quantities")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or hasAuthority('ROLE_WORKER')")
    @PostMapping("/{id}/count")
    public R<Void> submitCounts(@PathVariable Long id, @RequestBody CountRequest req) {
        List<StocktakeService.CountEntry> entries = req.getItems().stream()
                .map(it -> new StocktakeService.CountEntry(it.getItemId(), it.getCountQty(), it.getRemark()))
                .toList();
        stocktakeService.submitCounts(id, entries, null);
        return R.ok();
    }

    @Operation(summary = "Confirm stocktake (apply diff to stock)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping("/{id}/confirm")
    public R<Void> confirm(@PathVariable Long id) {
        stocktakeService.confirm(id, null);
        return R.ok();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    public R<Void> cancel(@PathVariable Long id) {
        stocktakeService.cancel(id);
        return R.ok();
    }

    @Data
    public static class CreateRequest {
        private Long warehouseId;
        private String countType;
        private String remark;
    }
    @Data
    public static class CountRequest {
        private List<CountItemDTO> items;
    }
    @Data
    public static class CountItemDTO {
        private Long itemId;
        private BigDecimal countQty;
        private String remark;
    }
}
