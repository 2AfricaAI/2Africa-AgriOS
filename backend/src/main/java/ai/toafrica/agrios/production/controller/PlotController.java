package ai.toafrica.agrios.production.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.framework.importer.ImportResult;
import ai.toafrica.agrios.framework.importer.ImportRunner;
import ai.toafrica.agrios.production.dto.PlotDTO;
import ai.toafrica.agrios.production.entity.Plot;
import ai.toafrica.agrios.production.mapper.PlotMapper;
import ai.toafrica.agrios.production.service.PlotService;
import ai.toafrica.agrios.production.service.importer.PlotImportTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "10 · Production-Plots", description = "Plot management")
@RestController
@RequestMapping("/v1/production/plots")
@RequiredArgsConstructor
public class PlotController {

    private final PlotService plotService;
    private final ImportRunner importRunner;
    private final PlotImportTemplate plotImportTemplate;

    @Operation(summary = "Plot list (paginated)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or hasAuthority('ROLE_LEADER') or hasAuthority('ROLE_PACKHOUSE') or hasAuthority('ROLE_SALES')")
    @GetMapping
    public R<PageResult<PlotMapper.PlotVO>> list(PlotMapper.PlotQueryVO q, PageQuery pq) {
        return R.ok(plotService.page(q, pq));
    }

    @Operation(summary = "Plot detail")
    @GetMapping("/{id}")
    public R<Plot> get(@PathVariable Long id) {
        return R.ok(plotService.detail(id));
    }

    @Operation(summary = "Plot stats over the last 30 days")
    @GetMapping("/{id}/stats")
    public R<PlotMapper.PlotStatVO> stats(@PathVariable Long id) {
        return R.ok(plotService.stats(id));
    }

    @Operation(summary = "Create plot")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping
    public R<Long> create(@Valid @RequestBody PlotDTO dto) {
        return R.ok(plotService.create(dto));
    }

    @Operation(summary = "Edit plot")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody PlotDTO dto) {
        plotService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "Disable / enable / fallow")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping("/{id}/status/{status}")
    public R<Void> changeStatus(@PathVariable Long id, @PathVariable String status) {
        plotService.toggleStatus(id, status);
        return R.ok();
    }

    @Operation(summary = "Delete plot (soft delete)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        plotService.delete(id);
        return R.ok();
    }

    // ============================================================
    // Sprint 38e - Excel bulk import
    // ============================================================

    @Operation(summary = "Download a blank import template (xlsx)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @GetMapping("/import/template")
    public ResponseEntity<ByteArrayResource> importTemplate() {
        byte[] bytes = importRunner.buildTemplate(plotImportTemplate);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=plots_import_template.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new ByteArrayResource(bytes));
    }

    @Operation(summary = "Bulk import plots from xlsx")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<ImportResult> importPlots(@RequestPart("file") MultipartFile file) {
        return R.ok(importRunner.run(plotImportTemplate, file));
    }
}
