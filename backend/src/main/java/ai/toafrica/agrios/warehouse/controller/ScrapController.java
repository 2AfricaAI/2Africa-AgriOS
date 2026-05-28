package ai.toafrica.agrios.warehouse.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.warehouse.service.ScrapService;
import ai.toafrica.agrios.warehouse.vo.ScrapDetailVO;
import ai.toafrica.agrios.warehouse.vo.ScrapVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "34 · Warehouse Ops-Scrap", description = "Scrap create/confirm/cancel")
@RestController
@RequestMapping("/v1/warehouse/scrap")
@RequiredArgsConstructor
public class ScrapController {
    private final ScrapService scrapService;

    @GetMapping
    public R<PageResult<ScrapVO>> list(@RequestParam(required = false) String status,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String scrapType, PageQuery pq) {
        return R.ok(scrapService.page(status, warehouseId, scrapType, pq));
    }

    @GetMapping("/{id}")
    public R<ScrapDetailVO> detail(@PathVariable Long id) { return R.ok(scrapService.detail(id)); }

    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping
    public R<Long> create(@RequestBody CreateScrapRequest req) {
        var items = req.getItems().stream()
                .map(it -> new ScrapService.ScrapItemInput(it.getInputItemId(), it.getQty(), it.getReason())).toList();
        return R.ok(scrapService.create(req.getWarehouseId(), req.getScrapType(), items, req.getRemark()));
    }

    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping("/{id}/confirm")
    public R<Void> confirm(@PathVariable Long id) { scrapService.confirm(id, null); return R.ok(); }

    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id) { scrapService.cancel(id); return R.ok(); }

    @Data
    public static class CreateScrapRequest {
        private Long warehouseId;
        private String scrapType;
        private String remark;
        private List<ScrapItemDTO> items;
    }
    @Data
    public static class ScrapItemDTO { private Long inputItemId; private BigDecimal qty; private String reason; }
}
