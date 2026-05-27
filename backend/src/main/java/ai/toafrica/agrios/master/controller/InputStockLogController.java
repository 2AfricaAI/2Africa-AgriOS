package ai.toafrica.agrios.master.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.master.service.InputStockLogService;
import ai.toafrica.agrios.master.vo.InputStockLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "28 · 仓库-出入库流水", description = "出入库流水日志 (只读, 审计)")
@RestController
@RequestMapping("/v1/warehouse/stock-log")
@RequiredArgsConstructor
public class InputStockLogController {

    private final InputStockLogService logService;

    @Operation(summary = "出入库流水列表 (分页 + 过滤)")
    @GetMapping
    public R<PageResult<InputStockLogVO>> list(
            @Parameter(description = "Filter by input_item.id")
                @RequestParam(required = false) Long inputItemId,
            @Parameter(description = "Filter by warehouse.id")
                @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "IN or OUT")
                @RequestParam(required = false) String direction,
            @Parameter(description = "po_receive / activity_consume / manual / ...")
                @RequestParam(required = false) String reasonType,
            PageQuery pq) {
        return R.ok(logService.page(inputItemId, warehouseId, direction, reasonType, pq));
    }
}
