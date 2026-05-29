package ai.toafrica.agrios.packhouse.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.framework.importer.ImportResult;
import ai.toafrica.agrios.framework.importer.ImportRunner;
import ai.toafrica.agrios.packhouse.service.InventoryService;
import ai.toafrica.agrios.packhouse.service.importer.OpeningInventoryImportTemplate;
import ai.toafrica.agrios.packhouse.vo.InventoryRow;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "16 · Packhouse-Inventory", description = "Operational inventory keyed by (SKU+batch+grade+location) 维度")
@RestController
@RequestMapping("/v1/packhouse/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final ImportRunner importRunner;
    private final OpeningInventoryImportTemplate openingInventoryTemplate;

    @Operation(summary = "Inventory list")
    @GetMapping
    public R<PageResult<InventoryRow>> list(
            @RequestParam(required = false) Long skuId,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Long locationId,
            @Parameter(description = "Grade A/B/C")
                @RequestParam(required = false) String grade,
            @Parameter(description = "normal/frozen/lost")
                @RequestParam(required = false) String status,
            PageQuery pq) {
        return R.ok(inventoryService.page(skuId, batchId, locationId, grade, status, pq));
    }

    // ============================================================
    // Sprint 38g - Opening-balance Excel import (one-shot at go-live)
    // ============================================================

    @Operation(summary = "Download opening-balance import template (xlsx)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @GetMapping("/import/template")
    public ResponseEntity<ByteArrayResource> openingTemplate() {
        byte[] bytes = importRunner.buildTemplate(openingInventoryTemplate);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=opening_inventory_template.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new ByteArrayResource(bytes));
    }

    @Operation(summary = "Bulk import opening inventory balances (idempotent on sku+wh+grade+expiry)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<ImportResult> importOpening(@RequestPart("file") MultipartFile file) {
        return R.ok(importRunner.run(openingInventoryTemplate, file));
    }
}
