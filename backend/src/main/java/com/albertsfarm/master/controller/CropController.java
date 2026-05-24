package com.albertsfarm.master.controller;

import com.albertsfarm.common.PageQuery;
import com.albertsfarm.common.PageResult;
import com.albertsfarm.common.R;
import com.albertsfarm.master.dto.CropForm;
import com.albertsfarm.master.entity.Crop;
import com.albertsfarm.master.service.CropService;
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

@Tag(name = "20 · 主数据-作物", description = "作物列表 / 详情")
@RestController
@RequestMapping("/v1/master/crops")
@RequiredArgsConstructor
public class CropController {

    private final CropService cropService;

    @Operation(summary = "作物列表(分页 + 过滤)")
    @GetMapping
    public R<PageResult<Crop>> list(
            @Parameter(description = "名称模糊查询") @RequestParam(required = false) String name,
            @Parameter(description = "编码模糊查询") @RequestParam(required = false) String code,
            @Parameter(description = "分类(叶菜/果蔬/根茎...)") @RequestParam(required = false) String category,
            @Parameter(description = "状态 1=启用 0=停用") @RequestParam(required = false) Integer status,
            PageQuery pq) {
        return R.ok(cropService.page(name, code, category, status, pq));
    }

    @Operation(summary = "作物详情")
    @GetMapping("/{id}")
    public R<Crop> detail(@PathVariable Long id) {
        return R.ok(cropService.detail(id));
    }

    @Operation(summary = "新建作物")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping
    public R<Long> create(@Valid @RequestBody CropForm form) {
        return R.ok(cropService.create(form));
    }

    @Operation(summary = "修改作物")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody CropForm form) {
        cropService.update(id, form);
        return R.ok();
    }

    @Operation(summary = "启用 / 停用 (status: 1=启用 0=停用)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @PostMapping("/{id}/status/{status}")
    public R<Void> changeStatus(@PathVariable Long id, @PathVariable Integer status) {
        cropService.changeStatus(id, status);
        return R.ok();
    }
}
