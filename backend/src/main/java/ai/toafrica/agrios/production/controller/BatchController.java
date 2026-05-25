package ai.toafrica.agrios.production.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.production.service.BatchService;
import ai.toafrica.agrios.production.vo.BatchVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "14 · 生产-批次追溯", description = "批次是从采收到出库的全链路追溯主键")
@RestController
@RequestMapping("/v1/production/batches")
@RequiredArgsConstructor
public class BatchController {

    private final BatchService batchService;

    @Operation(summary = "批次列表(分页 + 过滤)")
    @GetMapping
    public R<PageResult<BatchVO>> list(
            @RequestParam(required = false) Long plotId,
            @RequestParam(required = false) Long planId,
            @Parameter(description = "状态 pending/processing/packed/sold_out/lost")
                @RequestParam(required = false) String status,
            @RequestParam(required = false) String code,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            PageQuery pq) {
        return R.ok(batchService.page(plotId, planId, status, dateFrom, dateTo, code, pq));
    }

    @Operation(summary = "批次详情")
    @GetMapping("/{id}")
    public R<BatchVO> detail(@PathVariable Long id) {
        return R.ok(batchService.detail(id));
    }

    @Operation(summary = "切换批次状态 (pending/processing/packed/sold_out/lost)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or hasAuthority('ROLE_PACKHOUSE')")
    @PostMapping("/{id}/status/{status}")
    public R<Void> changeStatus(@PathVariable Long id, @PathVariable String status) {
        batchService.changeStatus(id, status);
        return R.ok();
    }
}
