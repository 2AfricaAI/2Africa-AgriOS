package ai.toafrica.agrios.qc.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.qc.dto.RecallForm;
import ai.toafrica.agrios.qc.service.RecallPdfService;
import ai.toafrica.agrios.qc.service.RecallService;
import ai.toafrica.agrios.qc.vo.RecallDetailVO;
import ai.toafrica.agrios.qc.vo.RecallVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;

@Tag(name = "45 · QC-Recall", description = "Batch recall — quarantine + downstream notification")
@RestController
@RequestMapping("/v1/qc/recalls")
@RequiredArgsConstructor
public class RecallController {

    private final RecallService recallService;
    private final RecallPdfService recallPdfService;

    @Operation(summary = "List recalls (paginated)")
    @GetMapping
    public R<PageResult<RecallVO>> list(
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) String status,
            PageQuery pq) {
        return R.ok(recallService.page(batchId, status, pq));
    }

    @Operation(summary = "Recall detail — header + affected orders")
    @GetMapping("/{id}")
    public R<RecallDetailVO> detail(@PathVariable Long id) {
        return R.ok(recallService.detail(id));
    }

    @Operation(summary = "Trigger a recall — freezes inventory + snapshots downstream orders")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or " +
                  "hasAuthority('ROLE_QC')")
    @PostMapping
    public R<Long> trigger(@Valid @RequestBody RecallForm form) {
        return R.ok(recallService.trigger(form));
    }

    @Operation(summary = "Mark a single affected customer as notified")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or " +
                  "hasAuthority('ROLE_SALES')")
    @PostMapping("/{id}/affected/{affectedOrderId}/notify")
    public R<Void> markNotified(@PathVariable Long id, @PathVariable Long affectedOrderId) {
        recallService.markNotified(id, affectedOrderId);
        return R.ok();
    }

    @Operation(summary = "Close a recall")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or " +
                  "hasAuthority('ROLE_QC')")
    @PostMapping("/{id}/close")
    public R<Void> close(@PathVariable Long id,
                         @RequestParam(required = false) String remark) {
        recallService.close(id, remark);
        return R.ok();
    }

    @Operation(summary = "Download recall report as PDF")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or " +
                  "hasAuthority('ROLE_QC') or hasAuthority('ROLE_SALES')")
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        byte[] data = recallPdfService.renderPdf(id);
        String filename = URLEncoder.encode("recall-" + id + ".pdf", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(data);
    }
}
