package ai.toafrica.agrios.system.service;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.framework.security.SecurityUtil;
import ai.toafrica.agrios.system.dto.CustomerSelfForm;
import ai.toafrica.agrios.system.dto.PartnerForm;
import ai.toafrica.agrios.system.dto.SysUserForm;
import ai.toafrica.agrios.system.entity.SysRole;
import ai.toafrica.agrios.system.entity.SysUser;
import ai.toafrica.agrios.system.entity.SysUserRole;
import ai.toafrica.agrios.system.entity.SysUserScope;
import ai.toafrica.agrios.system.mapper.SysRoleMapper;
import ai.toafrica.agrios.system.mapper.SysUserMapper;
import ai.toafrica.agrios.system.mapper.SysUserPartnerSubtypeMapper;
import ai.toafrica.agrios.system.mapper.SysUserRoleMapper;
import ai.toafrica.agrios.system.mapper.SysUserScopeMapper;
import ai.toafrica.agrios.system.vo.SysUserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * System user management — CRUD + role assignment + password reset (Sprint 34).
 *
 * All endpoints are SUPER_ADMIN-only or SUPER_ADMIN + MANAGER (enforced at
 * the controller layer with @PreAuthorize).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserPartnerSubtypeMapper subtypeMapper;
    private final SysUserScopeMapper scopeMapper;

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    // ------------------------------------------------------------
    // List
    // ------------------------------------------------------------
    public PageResult<SysUserVO> page(String username, String status, String userType, PageQuery pq) {
        QueryWrapper<SysUserVO> q = new QueryWrapper<>();
        q.isNull("u.deleted_at");
        if (username != null && !username.isBlank()) q.like("u.username", username.trim());
        if (status != null && !status.isBlank()) q.eq("u.status", status.trim());
        if (userType != null && !userType.isBlank()) q.eq("u.user_type", userType.trim());
        q.orderByDesc("u.id");
        Page<SysUserVO> p = new Page<>(pq.getPage(), pq.getSize());
        PageResult<SysUserVO> result = PageResult.of(userMapper.pageWithJoin(p, q));
        // Attach roles for each row
        for (SysUserVO vo : result.getList()) {
            vo.setRoles(loadRoleBriefs(vo.getId()));
        }
        return result;
    }

    public SysUserVO detail(Long id) {
        SysUser u = userMapper.selectById(id);
        if (u == null) throw new BusinessException(R.NOT_FOUND, "User not found");
        return toVO(u);
    }

    // ------------------------------------------------------------
    // Create
    // ------------------------------------------------------------
    @Transactional(rollbackFor = Exception.class)
    public Long create(SysUserForm form) {
        if (form.getPassword() == null || form.getPassword().isBlank()) {
            throw new BusinessException("Password is required when creating a user");
        }
        // Uniqueness check
        SysUser existing = userMapper.findByUsername(form.getUsername());
        if (existing != null) {
            throw new BusinessException("Username already taken: " + form.getUsername());
        }

        SysUser u = new SysUser();
        u.setUsername(form.getUsername());
        u.setPassword(ENCODER.encode(form.getPassword()));
        u.setNickname(form.getNickname());
        u.setPhone(form.getPhone());
        u.setEmail(form.getEmail());
        u.setStatus(form.getStatus() == null ? "active" : form.getStatus());
        u.setCreatedBy(SecurityUtil.currentUserId());
        userMapper.insert(u);

        replaceRoles(u.getId(), form.getRoleIds());

        log.info("[User created] id={} username={} roles={}", u.getId(), u.getUsername(), form.getRoleIds());
        return u.getId();
    }

    // ------------------------------------------------------------
    // Update — username and password are NOT touched here
    // ------------------------------------------------------------
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SysUserForm form) {
        SysUser u = userMapper.selectById(id);
        if (u == null) throw new BusinessException(R.NOT_FOUND, "User not found");

        u.setNickname(form.getNickname());
        u.setPhone(form.getPhone());
        u.setEmail(form.getEmail());
        if (form.getStatus() != null) u.setStatus(form.getStatus());
        userMapper.updateById(u);

        if (form.getRoleIds() != null) {
            replaceRoles(id, form.getRoleIds());
        }
        log.info("[User updated] id={} username={}", u.getId(), u.getUsername());
    }

    // ------------------------------------------------------------
    // Enable / disable
    // ------------------------------------------------------------
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, String status) {
        if (!List.of("active", "locked", "disabled").contains(status)) {
            throw new BusinessException("Invalid status: " + status);
        }
        SysUser u = userMapper.selectById(id);
        if (u == null) throw new BusinessException(R.NOT_FOUND, "User not found");
        if ("admin".equals(u.getUsername()) && !"active".equals(status)) {
            throw new BusinessException("Cannot disable the built-in admin user");
        }
        u.setStatus(status);
        userMapper.updateById(u);
        log.info("[User status changed] id={} -> {}", id, status);
    }

    // ------------------------------------------------------------
    // Reset password
    // ------------------------------------------------------------
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new BusinessException("Password must be at least 8 characters");
        }
        SysUser u = userMapper.selectById(id);
        if (u == null) throw new BusinessException(R.NOT_FOUND, "User not found");
        u.setPassword(ENCODER.encode(newPassword));
        userMapper.updateById(u);
        log.info("[Password reset] user id={}", id);
    }

    // ------------------------------------------------------------
    // Soft delete
    // ------------------------------------------------------------
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysUser u = userMapper.selectById(id);
        if (u == null) throw new BusinessException(R.NOT_FOUND, "User not found");
        if ("admin".equals(u.getUsername())) {
            throw new BusinessException("Cannot delete the built-in admin user");
        }
        userMapper.deleteById(id);    // TableLogic handles soft delete
        userRoleMapper.deleteByUserId(id);
        log.info("[User deleted] id={} username={}", id, u.getUsername());
    }

    // ------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------
    private void replaceRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.deleteByUserId(userId);
        if (roleIds == null || roleIds.isEmpty()) return;
        for (Long roleId : roleIds) {
            userRoleMapper.insertOne(userId, roleId);
        }
    }

    private SysUserVO toVO(SysUser u) {
        SysUserVO vo = new SysUserVO();
        vo.setId(u.getId());
        vo.setUsername(u.getUsername());
        vo.setNickname(u.getNickname());
        vo.setPhone(u.getPhone());
        vo.setEmail(u.getEmail());
        vo.setAvatar(u.getAvatar());
        vo.setStatus(u.getStatus());
        vo.setUserType(u.getUserType() == null ? "STAFF" : u.getUserType());
        vo.setOrgName(u.getOrgName());
        vo.setLinkedCustomerId(u.getLinkedCustomerId());
        vo.setLastLoginAt(u.getLastLoginAt());
        vo.setLastLoginIp(u.getLastLoginIp());
        vo.setCreatedAt(u.getCreatedAt());
        vo.setUpdatedAt(u.getUpdatedAt());
        vo.setRoles(loadRoleBriefs(u.getId()));
        vo.setPartnerSubtypes(subtypeMapper.findByUserId(u.getId()));
        return vo;
    }

    // ============================================================
    // Sprint 37 — PARTNER + CUSTOMER flows
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Long createPartner(PartnerForm form) {
        if (form.getPassword() == null || form.getPassword().isBlank()) {
            throw new BusinessException("Password is required");
        }
        if (form.getSubtypes() == null || form.getSubtypes().isEmpty()) {
            throw new BusinessException("At least one partner subtype is required");
        }
        if (userMapper.findByUsername(form.getUsername()) != null) {
            throw new BusinessException("Username already taken: " + form.getUsername());
        }
        SysUser u = new SysUser();
        u.setUsername(form.getUsername());
        u.setPassword(ENCODER.encode(form.getPassword()));
        u.setNickname(form.getNickname());
        u.setPhone(form.getPhone());
        u.setEmail(form.getEmail());
        u.setStatus("active");
        u.setUserType("PARTNER");
        u.setOrgName(form.getOrgName());
        u.setCreatedBy(SecurityUtil.currentUserId());
        userMapper.insert(u);

        // Subtypes
        setPartnerSubtypes(u.getId(), form.getSubtypes());

        // Roles: explicit roleIds win; else default-map each subtype to its role
        List<Long> roleIds = form.getRoleIds();
        if (roleIds == null || roleIds.isEmpty()) {
            roleIds = new java.util.ArrayList<>();
            for (String subtype : form.getSubtypes()) {
                Long roleId = roleMapper.findIdByCode(PartnerSubtype.defaultRoleCodeFor(subtype));
                if (roleId != null) roleIds.add(roleId);
            }
        }
        replaceRoles(u.getId(), roleIds);

        // Scopes
        if (form.getScopes() != null) {
            setUserScopes(u.getId(), form.getScopes());
        }

        log.info("[Partner created] id={} user={} subtypes={} org={}",
                u.getId(), u.getUsername(), form.getSubtypes(), form.getOrgName());
        return u.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createCustomerAccount(CustomerSelfForm form) {
        if (form.getPassword() == null || form.getPassword().isBlank()) {
            throw new BusinessException("Password is required");
        }
        if (userMapper.findByUsername(form.getUsername()) != null) {
            throw new BusinessException("Username already taken: " + form.getUsername());
        }
        SysUser u = new SysUser();
        u.setUsername(form.getUsername());
        u.setPassword(ENCODER.encode(form.getPassword()));
        u.setNickname(form.getNickname());
        u.setPhone(form.getPhone());
        u.setEmail(form.getEmail());
        u.setStatus("active");
        u.setUserType("CUSTOMER");
        u.setLinkedCustomerId(form.getLinkedCustomerId());
        u.setCreatedBy(SecurityUtil.currentUserId());
        userMapper.insert(u);

        // Bind CUSTOMER_SELF role
        Long roleId = roleMapper.findIdByCode("CUSTOMER_SELF");
        if (roleId != null) {
            userRoleMapper.insertOne(u.getId(), roleId);
        }
        log.info("[Customer-self account created] id={} user={} -> customer_id={}",
                u.getId(), u.getUsername(), form.getLinkedCustomerId());
        return u.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void setPartnerSubtypes(Long userId, List<String> subtypes) {
        subtypeMapper.deleteByUserId(userId);
        if (subtypes == null) return;
        for (String s : subtypes) subtypeMapper.insertOne(userId, s);
    }

    @Transactional(rollbackFor = Exception.class)
    public void setUserScopes(Long userId, List<PartnerForm.ScopeRow> rows) {
        scopeMapper.deleteByUserId(userId);
        if (rows == null) return;
        for (PartnerForm.ScopeRow r : rows) {
            SysUserScope s = new SysUserScope();
            s.setUserId(userId);
            s.setScopeType(r.getScopeType());
            s.setScopeId(r.getScopeId());
            s.setValidFrom(r.getValidFrom());
            s.setValidTo(r.getValidTo());
            scopeMapper.insert(s);
        }
    }

    public List<SysUserScope> userScopes(Long userId) {
        return scopeMapper.findByUserId(userId);
    }

    private List<SysUserVO.RoleBrief> loadRoleBriefs(Long userId) {
        List<SysRole> roles = roleMapper.findByUserId(userId);
        return roles.stream().map(r -> {
            SysUserVO.RoleBrief b = new SysUserVO.RoleBrief();
            b.setId(r.getId());
            b.setCode(r.getCode());
            b.setName(r.getName());
            return b;
        }).toList();
    }
}
