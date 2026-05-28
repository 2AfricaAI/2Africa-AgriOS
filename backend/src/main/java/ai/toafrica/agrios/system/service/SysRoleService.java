package ai.toafrica.agrios.system.service;

import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.system.dto.SysRoleForm;
import ai.toafrica.agrios.system.entity.SysRole;
import ai.toafrica.agrios.system.mapper.SysRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sprint 36: role catalogue management.
 *
 * Built-in roles cannot be renamed, re-coded, or deleted.  Their menu/perms
 * bindings can still be changed by SUPER_ADMIN via SysMenuService — that
 * decision is owner's call and not enforced here.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleService {

    private final SysRoleMapper roleMapper;

    @Transactional(rollbackFor = Exception.class)
    public Long create(SysRoleForm form) {
        long count = roleMapper.selectCount(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, form.getCode()));
        if (count > 0) {
            throw new BusinessException("Role code already exists: " + form.getCode());
        }
        SysRole r = new SysRole();
        r.setCode(form.getCode());
        r.setName(form.getName());
        r.setDataScope(form.getDataScope() == null ? "self" : form.getDataScope());
        r.setIsBuiltIn(0);
        r.setRemark(form.getRemark());
        roleMapper.insert(r);
        log.info("[Custom role created] id={} code={}", r.getId(), r.getCode());
        return r.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SysRoleForm form) {
        SysRole r = roleMapper.selectById(id);
        if (r == null) throw new BusinessException(R.NOT_FOUND, "Role not found");
        if (r.getIsBuiltIn() != null && r.getIsBuiltIn() == 1) {
            throw new BusinessException("Built-in roles cannot be renamed");
        }
        // Code is immutable to avoid breaking @PreAuthorize references
        r.setName(form.getName());
        if (form.getDataScope() != null) r.setDataScope(form.getDataScope());
        r.setRemark(form.getRemark());
        roleMapper.updateById(r);
        log.info("[Custom role updated] id={}", id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysRole r = roleMapper.selectById(id);
        if (r == null) throw new BusinessException(R.NOT_FOUND, "Role not found");
        if (r.getIsBuiltIn() != null && r.getIsBuiltIn() == 1) {
            throw new BusinessException("Built-in roles cannot be deleted");
        }
        roleMapper.deleteById(id);
        log.info("[Custom role deleted] id={} code={}", id, r.getCode());
    }
}
