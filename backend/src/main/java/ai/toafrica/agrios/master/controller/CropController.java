package ai.toafrica.agrios.master.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.framework.importer.ImportResult;
import ai.toafrica.agrios.framework.importer.ImportRunner;
import ai.toafrica.agrios.master.dto.CropForm;
import ai.toafrica.agrios.master.entity.Crop;
import ai.toafrica.agrios.master.service.CropService;
import ai.toafrica.agrios.master.service.importer.CropImportTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "20 · Master-Crops", description = "Crops list / detail")
@RestController
@RequestMapping("/v1/master/crops")
@RequiredArgsConstructor
public class CropController {

    private final CropService cropService;
    private final ImportRunner importRunner;
    private final CropImportTemplate cropImportTemplate;

    @Operation(summary = "Crop list (paginated + filtered)")
    @GetMapping
    public R<PageResult<Crop>> list(
            @Parameter(description = "Name fuzzy match") @RequestParam(required = false) String name,
            @Parameter(description = "Code fuzzy match") @RequestParam(required = false) String code,
            @Parameter(description = "Category (leafy / fruit / root ...)") @RequestParam(required = false) String category,
            @Parameter(description = "Status: 1=enabled, 0=disabled") @RequestParam(required = false) Integer status,
            PageQuery pq) {
        return R.ok(cropService.page(name, code, category, status, pq));
    }

    @Operation(summary = "Crop detail")
    @GetMapping("/{id}")
    public R<Crop> detail(@PathVariable Long id) {
        return R.ok(cropService.detail(id));
    }

    @Operation(summary = "Create crop")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping
    public R<Long> create(@Valid @RequestBody CropForm form) {
        return R.ok(cropService.create(form));
    }

    @Operation(summary = "Update crop")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody CropForm form) {
        cropService.update(id, form);
        return R.ok();
    }

    @Operation(summary = "Enable / disable (status: 1=enabled, 0=disabled)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping("/{id}/status/{status}")
    public R<Void> changeStatus(@PathVariable Long id, @PathVariable Integer status) {
        cropService.changeStatus(id, status);
        return R.ok();
    }

    // ============================================================
    // Sprint 38 - Excel bulk import
    // ============================================================

    @Operation(summary = "Download a blank import template (xlsx)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @GetMapping("/import/template")
    public ResponseEntity<ByteArrayResource> importTemplate() {
        byte[] bytes = importRunner.buildTemplate(cropImportTemplate);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=crops_import_template.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new ByteArrayResource(bytes));
    }

    @Operation(summary = "Bulk import crops from an xlsx file")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<ImportResult> importCrops(@RequestPart("file") MultipartFile file) {
        return R.ok(importRunner.run(cropImportTemplate, file));
    }
}
