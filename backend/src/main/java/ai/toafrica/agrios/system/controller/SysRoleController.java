package ai.toafrica.agrios.system.controller;

import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.system.dto.SysRoleForm;
import ai.toafrica.agrios.system.entity.SysRole;
import ai.toafrica.agrios.system.mapper.SysRoleMapper;
import ai.toafrica.agrios.system.service.ModulePermMatrix;
import ai.toafrica.agrios.system.service.SysMenuService;
import ai.toafrica.agrios.system.service.SysRoleService;
import ai.toafrica.agrios.system.vo.MenuTreeVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Role catalogue + menu/module assignment.
 *
 * Sprint 34: read-only role list.
 * Sprint 35: menu-tree assignment (kept as advanced / power-user view).
 * Sprint 36: simplified "module x 3-tier" assignment + custom-role CRUD.
 */
@Tag(name = "04 · System-Roles", description = "Role catalogue + access assignment")
@RestController
@RequestMapping("/v1/system/roles")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleMapper roleMapper;
    private final SysMenuService menuService;
    private final SysRoleService roleService;

    // ------------------------------------------------------------
    // List / detail
    // ------------------------------------------------------------

    @Operation(summary = "List all roles (built-in + custom)")
    @PreAuthorize("hasAuthority('system:role:list')")
    @GetMapping
    public R<List<SysRole>> list() {
        return R.ok(roleMapper.selectList(
                new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId)));
    }

    @Operation(summary = "Role detail")
    @PreAuthorize("hasAuthority('system:role:list')")
    @GetMapping("/{id}")
    public R<SysRole> detail(@PathVariable Long id) {
        return R.ok(roleMapper.selectById(id));
    }

    // ------------------------------------------------------------
    // Sprint 36 — custom role CRUD (only custom roles can be touched)
    // ------------------------------------------------------------

    @Operation(summary = "Create a custom role")
    @PreAuthorize("hasAuthority('system:role:assign-menu')")
    @PostMapping
    public R<Long> create(@Valid @RequestBody SysRoleForm form) {
        return R.ok(roleService.create(form));
    }

    @Operation(summary = "Rename / re-scope a custom role")
    @PreAuthorize("hasAuthority('system:role:assign-menu')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody SysRoleForm form) {
        roleService.update(id, form);
        return R.ok();
    }

    @Operation(summary = "Delete a custom role (built-in roles rejected)")
    @PreAuthorize("hasAuthority('system:role:assign-menu')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return R.ok();
    }

    // ------------------------------------------------------------
    // Sprint 36 — module x 3-tier access (primary UI)
    // ------------------------------------------------------------

    @Operation(summary = "List the 11 modules used by the simplified assignment UI")
    @PreAuthorize("hasAuthority('system:role:assign-menu')")
    @GetMapping("/modules")
    public R<List<String>> modules() {
        return R.ok(ModulePermMatrix.MODULES);
    }

    @Operation(summary = "Get a role's module-level access (none / read / write per module)")
    @PreAuthorize("hasAuthority('system:role:assign-menu')")
    @GetMapping("/{id}/module-access")
    public R<Map<String, String>> moduleAccess(@PathVariable Long id) {
        return R.ok(menuService.moduleAccess(id));
    }

    @Operation(summary = "Replace a role's module-level access (Stripe-style 11 x 3 matrix)")
    @PreAuthorize("hasAuthority('system:role:assign-menu')")
    @PutMapping("/{id}/module-access")
    public R<Void> setModuleAccess(@PathVariable Long id, @RequestBody Map<String, String> body) {
        menuService.setModuleAccess(id, body);
        return R.ok();
    }

    // ------------------------------------------------------------
    // Sprint 35 — power-user menu-tree assignment (kept as fallback)
    // ------------------------------------------------------------

    @Operation(summary = "Full menu tree (advanced view)")
    @PreAuthorize("hasAuthority('system:role:assign-menu')")
    @GetMapping("/menus")
    public R<List<MenuTreeVO>> menuTree() {
        return R.ok(menuService.tree());
    }

    @Operation(summary = "Menu IDs currently bound to a role (advanced view)")
    @PreAuthorize("hasAuthority('system:role:assign-menu')")
    @GetMapping("/{id}/menus")
    public R<List<Long>> roleMenuIds(@PathVariable Long id) {
        return R.ok(menuService.menuIdsByRoleId(id));
    }

    @Operation(summary = "Replace a role's menu bindings (advanced view)")
    @PreAuthorize("hasAuthority('system:role:assign-menu')")
    @PutMapping("/{id}/menus")
    public R<Void> assignMenus(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        menuService.assignMenus(id, body.get("menuIds"));
        return R.ok();
    }
}
