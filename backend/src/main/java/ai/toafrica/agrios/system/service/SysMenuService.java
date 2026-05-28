package ai.toafrica.agrios.system.service;

import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.system.entity.SysMenu;
import ai.toafrica.agrios.system.mapper.SysMenuMapper;
import ai.toafrica.agrios.system.mapper.SysRoleMapper;
import ai.toafrica.agrios.system.mapper.SysRoleMenuMapper;
import ai.toafrica.agrios.system.vo.MenuTreeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Menu definition + role-menu assignment (Sprint 35).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysMenuService {

    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysRoleMapper roleMapper;

    /** Full menu tree for the assignment UI. */
    public List<MenuTreeVO> tree() {
        List<SysMenu> flat = menuMapper.listAll();
        Map<Long, MenuTreeVO> byId = new HashMap<>();
        for (SysMenu m : flat) {
            MenuTreeVO v = new MenuTreeVO();
            v.setId(m.getId());
            v.setParentId(m.getParentId());
            v.setCode(m.getCode());
            v.setName(m.getName());
            v.setType(m.getType());
            v.setPath(m.getPath());
            v.setPerms(m.getPerms());
            v.setIcon(m.getIcon());
            v.setSort(m.getSort());
            byId.put(m.getId(), v);
        }
        List<MenuTreeVO> roots = new ArrayList<>();
        for (MenuTreeVO v : byId.values()) {
            if (v.getParentId() == null || v.getParentId() == 0) {
                roots.add(v);
            } else {
                MenuTreeVO p = byId.get(v.getParentId());
                if (p != null) p.getChildren().add(v);
                else roots.add(v); // orphan -> surface at top
            }
        }
        Comparator<MenuTreeVO> bySort = Comparator.comparing(
                v -> v.getSort() == null ? Integer.MAX_VALUE : v.getSort());
        roots.sort(bySort);
        for (MenuTreeVO v : byId.values()) {
            if (!v.getChildren().isEmpty()) v.getChildren().sort(bySort);
        }
        return roots;
    }

    /** Menu IDs currently bound to a role. */
    public List<Long> menuIdsByRoleId(Long roleId) {
        if (roleMapper.selectById(roleId) == null) {
            throw new BusinessException(R.NOT_FOUND, "Role not found");
        }
        return menuMapper.findMenuIdsByRoleId(roleId);
    }

    /** Replace the role's menu bindings.  Empty list clears everything. */
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds) {
        if (roleMapper.selectById(roleId) == null) {
            throw new BusinessException(R.NOT_FOUND, "Role not found");
        }
        roleMenuMapper.deleteByRoleId(roleId);
        if (menuIds == null || menuIds.isEmpty()) {
            log.info("[Role menus cleared] roleId={}", roleId);
            return;
        }
        for (Long mid : menuIds) {
            roleMenuMapper.insertOne(roleId, mid);
        }
        log.info("[Role menus updated] roleId={} count={}", roleId, menuIds.size());
    }

    // ============================================================
    // Sprint 36 — module-level (3-tier) access
    // ============================================================

    /**
     * Translate a role's existing sys_role_menu rows into a
     *   { module -> NONE/READ/WRITE } map for the simplified UI.
     *
     * A module is WRITE iff every menu in its read-list is bound AND at least
     * one of its write buttons is bound.
     * A module is READ iff every menu in its read-list is bound but no write
     * buttons are.
     * Else NONE.
     */
    public Map<String, String> moduleAccess(Long roleId) {
        if (roleMapper.selectById(roleId) == null) {
            throw new BusinessException(R.NOT_FOUND, "Role not found");
        }
        Set<String> boundCodes = new HashSet<>(menuMapper.findCodesByRoleId(roleId));
        Map<String, String> result = new LinkedHashMap<>();
        for (String module : ModulePermMatrix.MODULES) {
            List<String> readMenus = ModulePermMatrix.readMenuCodes(module);
            boolean hasAllReads = !readMenus.isEmpty() && boundCodes.containsAll(readMenus);
            boolean hasAnyWriteBtn = ModulePermMatrix.writeButtonPrefixes(module).stream()
                    .anyMatch(prefix -> boundCodes.stream().anyMatch(c -> c.startsWith(prefix)));
            String level = hasAllReads
                    ? (hasAnyWriteBtn ? "write" : "read")
                    : "none";
            result.put(module, level);
        }
        return result;
    }

    /**
     * Translate { module -> none/read/write } back into sys_role_menu rows.
     * Replaces the entire role's bindings.  The implementation:
     *   1. For each module set to READ/WRITE, look up read-list menu codes.
     *   2. For WRITE, also pull in all button menus whose code starts with the
     *      module's write-prefix list.
     *   3. UNION the resulting menu ids and replace sys_role_menu in one txn.
     */
    @Transactional(rollbackFor = Exception.class)
    public void setModuleAccess(Long roleId, Map<String, String> levels) {
        if (roleMapper.selectById(roleId) == null) {
            throw new BusinessException(R.NOT_FOUND, "Role not found");
        }
        Set<String> targetCodes = new HashSet<>();
        Set<String> writePrefixes = new HashSet<>();

        for (Map.Entry<String, String> e : levels.entrySet()) {
            String module = e.getKey();
            String level = e.getValue() == null ? "none" : e.getValue().toLowerCase();
            if (!ModulePermMatrix.MODULES.contains(module)) continue;
            if ("none".equals(level)) continue;

            // dir node (parent menu) — always include if the module has any access
            targetCodes.add(module);
            targetCodes.addAll(ModulePermMatrix.readMenuCodes(module));

            if ("write".equals(level)) {
                writePrefixes.addAll(ModulePermMatrix.writeButtonPrefixes(module));
            }
        }

        Set<Long> targetIds = new HashSet<>();
        if (!targetCodes.isEmpty()) {
            targetIds.addAll(menuMapper.findIdsByCodes(targetCodes));
        }
        if (!writePrefixes.isEmpty()) {
            targetIds.addAll(menuMapper.findButtonIdsByPrefixes(writePrefixes));
        }
        assignMenus(roleId, new ArrayList<>(targetIds));
        log.info("[Module access set] roleId={} levels={} -> {} menu ids",
                roleId, levels, targetIds.size());
    }
}
