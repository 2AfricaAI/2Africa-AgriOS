package ai.toafrica.agrios.packhouse.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.packhouse.dto.PackingForm;
import ai.toafrica.agrios.packhouse.service.PackingService;
import ai.toafrica.agrios.packhouse.vo.PackingRow;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "15 · Packhouse-Packing", description = "Packing creation transaction: decrement batch remaining + add inventory + write adjust log")
@RestController
@RequestMapping("/v1/packhouse/packings")
@RequiredArgsConstructor
public class PackingController {

    private final PackingService packingService;

    @Operation(summary = "Packing list")
    @GetMapping
    public R<PageResult<PackingRow>> list(
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Long skuId,
            @RequestParam(required = false) Long locationId,
            @Parameter(description = "Grade A/B/C")
                @RequestParam(required = false) String grade,
            PageQuery pq) {
        return R.ok(packingService.page(batchId, skuId, locationId, grade, pq));
    }

    @Operation(summary = "Create packing (decrement batch + add inventory)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or hasAuthority('ROLE_PACKHOUSE')")
    @PostMapping
    public R<Long> create(@Valid @RequestBody PackingForm form) {
        return R.ok(packingService.create(form));
    }
}
