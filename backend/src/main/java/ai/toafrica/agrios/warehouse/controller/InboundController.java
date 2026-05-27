package ai.toafrica.agrios.warehouse.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.warehouse.service.InboundService;
import ai.toafrica.agrios.warehouse.vo.InboundDetailVO;
import ai.toafrica.agrios.warehouse.vo.InboundVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "30 · 仓库作业-入库", description = "入库单列表/详情/确认/取消")
@RestController
@RequestMapping("/v1/warehouse/inbound")
@RequiredArgsConstructor
public class InboundController {

    private final InboundService inboundService;

    @Operation(summary = "入库单列表")
    @GetMapping
    public R<PageResult<InboundVO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String sourceType,
            PageQuery pq) {
        return R.ok(inboundService.page(status, warehouseId, sourceType, pq));
    }

    @Operation(summary = "入库单详情 (含明细行)")
    @GetMapping("/{id}")
    public R<InboundDetailVO> detail(@PathVariable Long id) {
        return R.ok(inboundService.detail(id));
    }

    @Operation(summary = "确认入库 (仓库人员填写实际数量)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping("/{id}/confirm")
    public R<Void> confirm(@PathVariable Long id, @RequestBody ConfirmRequest req) {
        List<InboundService.ConfirmItem> items = req.getItems().stream()
                .map(it -> new InboundService.ConfirmItem(it.getItemId(), it.getActualQty(), it.getRemark()))
                .toList();
        inboundService.confirm(id, items, null); // TODO: 从 SecurityContext 取 operatorId
        return R.ok();
    }

    @Operation(summary = "取消入库单 (仅 draft)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id) {
        inboundService.cancel(id);
        return R.ok();
    }

    @Data
    public static class ConfirmRequest {
        private List<ConfirmItemDTO> items;
    }

    @Data
    public static class ConfirmItemDTO {
        private Long itemId;
        private BigDecimal actualQty;
        private String remark;
    }
}
