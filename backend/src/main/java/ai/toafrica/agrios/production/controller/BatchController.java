package ai.toafrica.agrios.production.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.production.dto.BatchSplitForm;
import ai.toafrica.agrios.production.service.BatchService;
import ai.toafrica.agrios.production.vo.BatchDetailVO;
import ai.toafrica.agrios.production.vo.BatchVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "14 · Production-Batches", description = "Batch is the full-chain traceability key from harvest to outbound")
@RestController
@RequestMapping("/v1/production/batches")
@RequiredArgsConstructor
public class BatchController {

    private final BatchService batchService;

    @Operation(summary = "Batch list (paginated + filtered)")
    @GetMapping
    public R<PageResult<BatchVO>> list(
            @RequestParam(required = false) Long plotId,
            @RequestParam(required = false) Long planId,
            @Parameter(description = "Status pending/processing/packed/sold_out/lost")
                @RequestParam(required = false) String status,
            @RequestParam(required = false) String code,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            PageQuery pq) {
        return R.ok(batchService.page(plotId, planId, status, dateFrom, dateTo, code, pq));
    }

    @Operation(summary = "Batch detail")
    @GetMapping("/{id}")
    public R<BatchVO> detail(@PathVariable Long id) {
        return R.ok(batchService.detail(id));
    }

    @Operation(summary = "Change batch status (pending/processing/packed/sold_out/lost)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or hasAuthority('ROLE_PACKHOUSE')")
    @PostMapping("/{id}/status/{status}")
    public R<Void> changeStatus(@PathVariable Long id, @PathVariable String status) {
        batchService.changeStatus(id, status);
        return R.ok();
    }

    @Operation(summary = "Batch detail (with parent/children/packings full chain)")
    @GetMapping("/{id}/detail")
    public R<BatchDetailVO> detailFull(@PathVariable Long id) {
        return R.ok(batchService.detailFull(id));
    }

    @Operation(summary = "Split batch - one into many")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or hasAuthority('ROLE_PACKHOUSE')")
    @PostMapping("/{id}/split")
    public R<List<Long>> split(@PathVariable Long id, @RequestBody @Valid BatchSplitForm form) {
        return R.ok(batchService.split(id, form));
    }
}
