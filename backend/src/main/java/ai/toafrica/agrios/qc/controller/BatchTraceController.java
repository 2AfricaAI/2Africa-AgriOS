package ai.toafrica.agrios.qc.controller;

import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.qc.service.BatchTraceService;
import ai.toafrica.agrios.qc.vo.TraceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 批次完整追溯链 - 内部页面 (Sprint 25).
 * 公开扫码版本在 PublicTraceController.
 */
@Tag(name = "42 · QC-Trace", description = "Batch full traceability (internal)")
@RestController
@RequestMapping("/v1/qc/trace")
@RequiredArgsConstructor
public class BatchTraceController {

    private final BatchTraceService traceService;

    @Operation(summary = "Get complete trace chain by batch code")
    @GetMapping("/{batchCode}")
    public R<TraceVO> trace(@PathVariable String batchCode) {
        return R.ok(traceService.trace(batchCode));
    }
}
