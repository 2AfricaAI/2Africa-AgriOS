package ai.toafrica.agrios.finance.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.finance.dto.CollectionLogForm;
import ai.toafrica.agrios.finance.service.CollectionLogService;
import ai.toafrica.agrios.finance.vo.CollectionLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "52 · Finance Collections", description = "AR 催收跟催记录")
@RestController
@RequestMapping("/v1/finance/collections")
@RequiredArgsConstructor
public class CollectionLogController {

    private final CollectionLogService service;

    @Operation(summary = "List collection logs (with customer + order join)")
    @GetMapping
    public R<PageResult<CollectionLogVO>> list(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String outcome,
            PageQuery pq) {
        return R.ok(service.page(customerId, orderId, outcome, pq));
    }

    @Operation(summary = "Full history for one customer (timeline)")
    @GetMapping("/by-customer/{customerId}")
    public R<List<CollectionLogVO>> byCustomer(@PathVariable Long customerId) {
        return R.ok(service.listByCustomer(customerId));
    }

    @Operation(summary = "Record a collection log")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or hasAuthority('ROLE_SALES') or hasAuthority('ROLE_FINANCE')")
    @PostMapping
    public R<Long> create(@Valid @RequestBody CollectionLogForm form) {
        return R.ok(service.create(form));
    }

    @Operation(summary = "Soft-delete a collection log")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }

    @Operation(summary = "Active promises (for 13-week cash flow forecast)")
    @GetMapping("/active-promises")
    public R<List<Map<String, Object>>> activePromises() {
        return R.ok(service.findActivePromises());
    }
}
