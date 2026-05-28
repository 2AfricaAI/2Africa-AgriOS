package ai.toafrica.agrios.qc.controller;

import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.qc.service.PhiCheckService;
import ai.toafrica.agrios.qc.vo.PhiCheckVO;
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

@Tag(name = "40 · QC-PHI 安全期检查", description = "Pre-Harvest Interval check (Sprint 23 / Phase 5)")
@RestController
@RequestMapping("/v1/qc/phi")
@RequiredArgsConstructor
public class PhiCheckController {

    private final PhiCheckService phiCheckService;

    @Operation(summary = "Check whether a plan can be harvested on the proposed date")
    @GetMapping("/check")
    public R<PhiCheckVO> check(
            @Parameter(description = "Planting plan id", required = true)
                @RequestParam Long planId,
            @Parameter(description = "Proposed harvest date (default: today)")
                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return R.ok(phiCheckService.checkPlan(planId, date));
    }

    @Operation(summary = "Check by plot id (cross-plan pesticide residue)")
    @GetMapping("/check-by-plot")
    public R<PhiCheckVO> checkByPlot(
            @RequestParam Long plotId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return R.ok(phiCheckService.checkPlot(plotId, date));
    }
}
