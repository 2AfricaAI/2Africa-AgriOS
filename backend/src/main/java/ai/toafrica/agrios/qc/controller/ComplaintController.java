package ai.toafrica.agrios.qc.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.qc.dto.ComplaintForm;
import ai.toafrica.agrios.qc.service.ComplaintService;
import ai.toafrica.agrios.qc.vo.ComplaintVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "44 · QC-Complaints", description = "Customer / QC complaints (Sprint 27)")
@RestController
@RequestMapping("/v1/qc/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    @Operation(summary = "List complaints (paginated)")
    @GetMapping
    public R<PageResult<ComplaintVO>> list(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Long batchId,
            @Parameter(description = "quality / quantity / late / safety / wrong_product / other")
                @RequestParam(required = false) String category,
            @Parameter(description = "low / medium / high / critical")
                @RequestParam(required = false) String severity,
            @Parameter(description = "open / investigating / resolved / closed / escalated_to_recall")
                @RequestParam(required = false) String status,
            PageQuery pq) {
        return R.ok(complaintService.page(customerId, orderId, batchId,
                category, severity, status, pq));
    }

    @Operation(summary = "Complaint detail")
    @GetMapping("/{id}")
    public R<ComplaintVO> detail(@PathVariable Long id) {
        return R.ok(complaintService.detail(id));
    }

    @Operation(summary = "Create a complaint")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or " +
                  "hasAuthority('ROLE_SALES') or hasAuthority('ROLE_QC')")
    @PostMapping
    public R<Long> create(@Valid @RequestBody ComplaintForm form) {
        return R.ok(complaintService.create(form));
    }

    @Operation(summary = "Update a complaint (only while not closed/escalated)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or " +
                  "hasAuthority('ROLE_SALES') or hasAuthority('ROLE_QC')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody ComplaintForm form) {
        complaintService.update(id, form);
        return R.ok();
    }

    @Operation(summary = "Transition status (investigating / resolved / closed)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or " +
                  "hasAuthority('ROLE_SALES') or hasAuthority('ROLE_QC')")
    @PostMapping("/{id}/transition")
    public R<Void> transition(@PathVariable Long id,
                              @RequestParam String to,
                              @RequestParam(required = false) String resolution) {
        complaintService.transition(id, to, resolution);
        return R.ok();
    }

    @Operation(summary = "Delete an open complaint")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        complaintService.delete(id);
        return R.ok();
    }
}
