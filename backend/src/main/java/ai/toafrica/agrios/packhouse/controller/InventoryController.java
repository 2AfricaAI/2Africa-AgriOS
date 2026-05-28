package ai.toafrica.agrios.packhouse.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.packhouse.service.InventoryService;
import ai.toafrica.agrios.packhouse.vo.InventoryRow;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "16 · Packhouse-Inventory", description = "Operational inventory keyed by (SKU+batch+grade+location) 维度")
@RestController
@RequestMapping("/v1/packhouse/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @Operation(summary = "Inventory list")
    @GetMapping
    public R<PageResult<InventoryRow>> list(
            @RequestParam(required = false) Long skuId,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Long locationId,
            @Parameter(description = "Grade A/B/C")
                @RequestParam(required = false) String grade,
            @Parameter(description = "normal/frozen/lost")
                @RequestParam(required = false) String status,
            PageQuery pq) {
        return R.ok(inventoryService.page(skuId, batchId, locationId, grade, status, pq));
    }
}
