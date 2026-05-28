package ai.toafrica.agrios.warehouse.controller;

import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.warehouse.service.WarehouseReportService;
import ai.toafrica.agrios.warehouse.vo.WarehouseReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "35 · 仓库作业-报表", description = "仓库作业聚合报表")
@RestController
@RequestMapping("/v1/warehouse/reports")
@RequiredArgsConstructor
public class WarehouseReportController {

    private final WarehouseReportService reportService;

    @Operation(summary = "Warehouse operations report (doc counts + top items + low stock + stock by warehouse)")
    @GetMapping
    public R<WarehouseReportVO> report(
            @Parameter(description = "Period start (default: today - 30)")
                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Period end exclusive (default: today + 1)")
                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return R.ok(reportService.report(from, to));
    }
}
