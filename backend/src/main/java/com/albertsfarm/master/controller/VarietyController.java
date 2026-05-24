package com.albertsfarm.master.controller;

import com.albertsfarm.common.PageQuery;
import com.albertsfarm.common.PageResult;
import com.albertsfarm.common.R;
import com.albertsfarm.master.dto.VarietyForm;
import com.albertsfarm.master.entity.Variety;
import com.albertsfarm.master.service.VarietyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "21 · 主数据-品种", description = "品种列表 / 详情")
@RestController
@RequestMapping("/v1/master/varieties")
@RequiredArgsConstructor
public class VarietyController {

    private final VarietyService varietyService;

    @Operation(summary = "品种列表(分页 + 过滤)")
    @GetMapping
    public R<PageResult<Variety>> list(
            @Parameter(description = "按作物 ID 过滤") @RequestParam(required = false) Long cropId,
            @Parameter(description = "名称模糊查询") @RequestParam(required = false) String name,
            @Parameter(description = "编码模糊查询") @RequestParam(required = false) String code,
            @Parameter(description = "状态 1=启用 0=停用") @RequestParam(required = false) Integer status,
            PageQuery pq) {
        return R.ok(varietyService.page(cropId, name, code, status, pq));
    }

    @Operation(summary = "品种详情")
    @GetMapping("/{id}")
    public R<Variety> detail(@PathVariable Long id) {
        return R.ok(varietyService.detail(id));
    }

    @Operation(summary = "新建品种")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping
    public R<Long> create(@Valid @RequestBody VarietyForm form) {
        return R.ok(varietyService.create(form));
    }

    @Operation(summary = "修改品种")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody VarietyForm form) {
        varietyService.update(id, form);
        return R.ok();
    }

    @Operation(summary = "启用 / 停用 (status: 1=启用 0=停用)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping("/{id}/status/{status}")
    public R<Void> changeStatus(@PathVariable Long id, @PathVariable Integer status) {
        varietyService.changeStatus(id, status);
        return R.ok();
    }
}
