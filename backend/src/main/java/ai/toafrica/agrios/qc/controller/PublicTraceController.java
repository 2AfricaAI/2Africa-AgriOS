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
 * 公开追溯端点 - 二维码扫描后无需登录 (Sprint 25).
 *
 * 路径 /v1/public/** 在 SecurityConfig 中已配置免认证.
 *
 * 返回与内部 trace 同一份 VO; 前端公开页面可决定屏蔽 GPS / operator 等敏感字段.
 */
@Tag(name = "43 · 公开-追溯链", description = "Public batch trace (no auth)")
@RestController
@RequestMapping("/v1/public/trace")
@RequiredArgsConstructor
public class PublicTraceController {

    private final BatchTraceService traceService;

    @Operation(summary = "Public trace by batch code — no authentication required")
    @GetMapping("/{batchCode}")
    public R<TraceVO> trace(@PathVariable String batchCode) {
        return R.ok(traceService.trace(batchCode));
    }
}
