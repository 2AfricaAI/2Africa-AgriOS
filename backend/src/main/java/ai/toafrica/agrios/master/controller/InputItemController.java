package ai.toafrica.agrios.master.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.master.dto.InputItemForm;
import ai.toafrica.agrios.master.service.InputItemService;
import ai.toafrica.agrios.master.vo.InputItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "投入品主数据 (Phase 4)")
@RestController
@RequestMapping("/v1/master/input-items")
@RequiredArgsConstructor
public class InputItemController {

    private final InputItemService service;

    @Operation(summary = "投入品列表 (分页 + 过滤)")
    @GetMapping
    public R<PageResult<InputItemVO>> list(
            @Parameter @RequestParam(required = false) String code,
            @Parameter @RequestParam(required = false) String name,
            @Parameter(description = "fertilizer/pesticide/seed/film/labor/other")
                @RequestParam(required = false) String inputType,
            @Parameter @RequestParam(required = false) Long supplierId,
            @Parameter(description = "active / inactive")
                @RequestParam(required = false) String status,
            PageQuery pq) {
        return R.ok(service.page(code, name, inputType, supplierId, status, pq));
    }

    @Operation(summary = "投入品详情")
    @GetMapping("/{id}")
    public R<InputItemVO> detail(@PathVariable Long id) {
        return R.ok(service.detail(id));
    }

    @Operation(summary = "新建投入品")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping
    public R<Long> create(@Valid @RequestBody InputItemForm form) {
        return R.ok(service.create(form));
    }

    @Operation(summary = "修改投入品")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody InputItemForm form) {
        service.update(id, form);
        return R.ok();
    }

    @Operation(summary = "启用/停用 (active|inactive)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping("/{id}/status/{status}")
    public R<Void> toggleStatus(@PathVariable Long id, @PathVariable String status) {
        service.toggleStatus(id, status);
        return R.ok();
    }

    @Operation(summary = "删除投入品 (仅 SUPER_ADMIN)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable 