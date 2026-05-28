package ai.toafrica.agrios.system.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.system.dto.CustomerSelfForm;
import ai.toafrica.agrios.system.dto.PartnerForm;
import ai.toafrica.agrios.system.dto.SysUserForm;
import ai.toafrica.agrios.system.entity.SysUserScope;
import ai.toafrica.agrios.system.service.PartnerSubtype;
import ai.toafrica.agrios.system.service.SysUserService;
import ai.toafrica.agrios.system.vo.SysUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "03 · System-Users", description = "System user management (Sprint 34)")
@RestController
@RequestMapping("/v1/system/users")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;

    @Operation(summary = "User list (paginated, filterable by user_type)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @GetMapping
    public R<PageResult<SysUserVO>> list(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String userType,
            PageQuery pq) {
        return R.ok(userService.page(username, status, userType, pq));
    }

    @Operation(summary = "User detail")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @GetMapping("/{id}")
    public R<SysUserVO> detail(@PathVariable Long id) {
        return R.ok(userService.detail(id));
    }

    @Operation(summary = "Create a user")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    @PostMapping
    public R<Long> create(@Valid @RequestBody SysUserForm form) {
        return R.ok(userService.create(form));
    }

    @Operation(summary = "Update a user (excludes password)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody SysUserForm form) {
        userService.update(id, form);
        return R.ok();
    }

    @Operation(summary = "Change user status (active / locked / disabled)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    @PostMapping("/{id}/status")
    public R<Void> changeStatus(@PathVariable Long id, @RequestParam String status) {
        userService.changeStatus(id, status);
        return R.ok();
    }

    @Operation(summary = "Reset password (admin sets a new one)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    @PostMapping("/{id}/reset-password")
    public R<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        userService.resetPassword(id, body.get("password"));
        return R.ok();
    }

    @Operation(summary = "Soft-delete a user (cannot delete the built-in admin)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return R.ok();
    }

    // ============================================================
    // Sprint 37 — Partner / Customer creation endpoints
    // ============================================================

    @Operation(summary = "List the well-known partner subtype codes for the form drop-down")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    @GetMapping("/partner-subtypes")
    public R<List<String>> partnerSubtypes() {
        return R.ok(PartnerSubtype.ALL);
    }

    @Operation(summary = "Create a PARTNER user (external agronomist / GAP auditor / bank / landlord / insurance)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    @PostMapping("/partners")
    public R<Long> createPartner(@Valid @RequestBody PartnerForm form) {
        return R.ok(userService.createPartner(form));
    }

    @Operation(summary = "Create a CUSTOMER self-service account linked to a customer row")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    @PostMapping("/customers")
    public R<Long> createCustomer(@Valid @RequestBody CustomerSelfForm form) {
        return R.ok(userService.createCustomerAccount(form));
    }

    @Operation(summary = "Get a user's per-row data scopes")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    @GetMapping("/{id}/scopes")
    public R<List<SysUserScope>> scopes(@PathVariable Long id) {
        return R.ok(userService.userScopes(id));
    }

    @Operation(summary = "Replace a user's per-row data scopes")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    @PutMapping("/{id}/scopes")
    public R<Void> setScopes(@PathVariable Long id, @RequestBody List<PartnerForm.ScopeRow> rows) {
        userService.setUserScopes(id, rows);
        return R.ok();
    }

    @Operation(summary = "Replace a partner user's subtypes (e.g. AGRONOMIST + GAP_AUDITOR)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    @PutMapping("/{id}/partner-subtypes")
    public R<Void> setPartnerSubtypes(@PathVariable Long id, @RequestBody List<String> subtypes) {
        userService.setPartnerSubtypes(id, subtypes);
        return R.ok();
    }
}
