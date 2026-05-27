package ai.toafrica.agrios.master.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.master.service.InputStockService;
import ai.toafrica.agrios.master.vo.InputStockVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "27 · 仓库-投入品库存", description = "投入品库存查询 (只读)")
@RestController
@RequestMapping("/v1/warehouse/input-stock")
@RequiredArgsConstructor
public class InputStockController {

    private final InputStockService stockService;

    @Operation(summary = "投入品库存列表 (分页 + 过滤)")
    @GetMapping
    public R<PageResult<InputStockVO>> list(
            @Parameter(description = "Filter by input_item.id")
                @RequestParam(required = false) Long inputItemId,
            @Parameter(description = "Filter by warehouse.id")
                @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "Filter by input_type (seed/fertilizer/pesticide/...)")
                @RequestParam(required = false) String inputType,
            @Parameter(description = "Only show items below min_stock_qty threshold")
                @RequestParam(required = false) Boolean lowStockOnly,
            PageQuery pq) {
        return R.ok(stockService.page(inputItemId, warehouseId, inputType, lowStockOnly, pq));
    }
}
