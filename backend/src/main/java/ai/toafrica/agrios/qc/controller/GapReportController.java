package ai.toafrica.agrios.qc.controller;

import ai.toafrica.agrios.qc.service.GapReportPdfService;
import ai.toafrica.agrios.qc.service.GapReportPeriodService;
import ai.toafrica.agrios.qc.service.GapReportService;
import ai.toafrica.agrios.qc.service.GapReportXlsxService;
import ai.toafrica.agrios.qc.vo.GapReportData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ai.toafrica.agrios.common.R;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "46 · QC-GAP Reports", description = "GAP / HACCP audit exports — per-batch PDF/Excel and period summary")
@RestController
@RequestMapping("/v1/qc/gap-reports")
@RequiredArgsConstructor
public class GapReportController {

    private final GapReportService gapReportService;
    private final GapReportPdfService pdfService;
    private final GapReportXlsxService xlsxService;
    private final GapReportPeriodService periodService;

    // ----- JSON preview (handy for verifying data before rendering) -----
    @Operation(summary = "Preview GAP report data as JSON (for debugging)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or " +
                  "hasAuthority('ROLE_QC')")
    @GetMapping("/batch/{batchCode}/data")
    public R<GapReportData> preview(@PathVariable String batchCode) {
        return R.ok(gapReportService.buildBatchReport(batchCode));
    }

    // ----- Per-batch PDF -----
    @Operation(summary = "Download per-batch GAP report as PDF")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or " +
                  "hasAuthority('ROLE_QC') or hasAuthority('ROLE_SALES')")
    @GetMapping("/batch/{batchCode}/pdf")
    public ResponseEntity<byte[]> batchPdf(@PathVariable String batchCode) {
        byte[] data = pdfService.render(batchCode);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"GAP-" + batchCode + ".pdf\"")
                .body(data);
    }

    // ----- Per-batch Excel -----
    @Operation(summary = "Download per-batch GAP report as Excel (.xlsx)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or " +
                  "hasAuthority('ROLE_QC') or hasAuthority('ROLE_SALES')")
    @GetMapping("/batch/{batchCode}/xlsx")
    public ResponseEntity<byte[]> batchXlsx(@PathVariable String batchCode) {
        byte[] data = xlsxService.render(batchCode);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"GAP-" + batchCode + ".xlsx\"")
                .body(data);
    }

    // ----- Period-level Excel -----
    @Operation(summary = "Download period-level GAP audit Excel (one row per batch)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or " +
                  "hasAuthority('ROLE_QC')")
    @GetMapping("/period/xlsx")
    public ResponseEntity<byte[]> periodXlsx(
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long cropId) {
        byte[] data = periodService.render(from, to, cropId);
        String name = String.format("GAP-period-%s-to-%s.xlsx",
                from == null ? "all" : from, to == null ? "all" : to);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .body(data);
    }
}
