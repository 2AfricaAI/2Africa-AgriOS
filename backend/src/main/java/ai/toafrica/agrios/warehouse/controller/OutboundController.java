package ai.toafrica.agrios.warehouse.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.warehouse.service.WarehouseOutboundService;
import ai.toafrica.agrios.warehouse.vo.OutboundDetailVO;
import ai.toafrica.agrios.warehouse.vo.OutboundVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "31 · 仓库作业-出库", description = "出库单列表/详情/拣货/确认/取消")
@RestController
@RequestMapping("/v1/warehouse/outbound")
@RequiredArgsConstructor
public class OutboundController {

    private final WarehouseOutboundService outboundService;

    @Operation(summary = "出库单列表")
    @GetMapping
    public R<PageResult<OutboundVO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String sourceType,
            PageQuery pq) {
        return R.ok(outboundService.page(status, warehouseId, sourceType, pq));
    }

    @Operation(summary = "出库单详情 (含明细行)")
    @GetMapping("/{id}")
    public R<OutboundDetailVO> detail(@PathVariable Long id) {
        return R.ok(outboundService.detail(id));
    }

    @Operation(summary = "拣货 (填 picked_qty)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or hasAuthority('ROLE_WORKER')")
    @PostMapping("/{id}/pick")
    public R<Void> pick(@PathVariable Long id, @RequestBody PickRequest req) {
        List<WarehouseOutboundService.PickItem> items = req.getItems().stream()
                .map(it -> new WarehouseOutboundService.PickItem(it.getItemId(), it.getPickedQty(), it.getRemark()))
                .toList();
        outboundService.pick(id, items, null);
        return R.ok();
    }

    @Operation(summary = "确认出库 (扣库存)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping("/{id}/confirm")
    public R<Void> confirm(@PathVariable Long id) {
        outboundService.confirm(id, null);
        return R.ok();
    }

    @Operation(summary = "取消出库单")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id) {
        outboundService.cancel(id);
        return R.ok();
    }

    @Operation(summary = "手工创建出库单")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping
    public R<Long> create(@RequestBody CreateOutboundRequest req) {
        List<WarehouseOutboundService.OutboundItemInput> items = req.getItems().stream()
                .map(it -> new WarehouseOutboundService.OutboundItemInput(it.getInputItemId(), it.getRequestedQty()))
                .toList();
        return R.ok(outboundService.create(
                req.getSourceType() != null ? req.getSourceType() : "manual",
                req.getSourceId(), req.getWarehouseId(), items));
    }

    @Data
    public static class PickRequest {
        private List<PickItemDTO> items;
    }
    @Data
    public static class PickItemDTO {
        private Long itemId;
        private BigDecimal pickedQty;
        private String remark;
    }
    @Data
    public static class CreateOutboundRequest {
        private String sourceType;
        private Long sourceId;
        private Long warehouseId;
        private List<CreateOutboundItemDTO> items;
    }
    @Data
    public static class CreateOutboundItemDTO {
        private Long inputItemId;
        private BigDecimal requestedQty;
    }
}
