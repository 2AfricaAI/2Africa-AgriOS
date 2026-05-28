package ai.toafrica.agrios.qc.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.qc.dto.QcInspectionForm;
import ai.toafrica.agrios.qc.service.QcInspectionService;
import ai.toafrica.agrios.qc.vo.QcInspectionDetailVO;
import ai.toafrica.agrios.qc.vo.QcInspectionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "41 · QC-质检单", description = "Quality Control inspections (Sprint 24 / Phase 5)")
@RestController
@RequestMapping("/v1/qc/inspections")
@RequiredArgsConstructor
public class QcInspectionController {

    private final QcInspectionService inspService;

    @Operation(summary = "List QC inspections")
    @GetMapping
    public R<PageResult<QcInspectionVO>> list(
            @RequestParam(required = false) String inspectionType,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String refType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            PageQuery pq) {
        return R.ok(inspService.page(inspectionType, result, refType, dateFrom, dateTo, pq));
    }

    @Operation(summary = "Inspection detail with items")
    @GetMapping("/{id}")
    public R<QcInspectionDetailVO> detail(@PathVariable Long id) {
        return R.ok(inspService.detail(id));
    }

    @Operation(summary = "Create QC inspection")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or hasAuthority('ROLE_QC')")
    @PostMapping
    public R<Long> create(@Valid @RequestBody QcInspectionForm form) {
        return R.ok(inspService.create(form, null));
    }

    @Operation(summary = "Update QC inspection")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or hasAuthority('ROLE_QC')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody QcInspectionForm form) {
        inspService.update(id, form);
        return R.ok();
    }

    @Operation(summary = "Delete QC inspection")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        inspService.delete(id);
        return R.ok();
    }
}
