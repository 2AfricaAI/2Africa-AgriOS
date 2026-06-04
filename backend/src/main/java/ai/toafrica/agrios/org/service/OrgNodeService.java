package ai.toafrica.agrios.org.service;

import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.org.entity.OrgNode;
import ai.toafrica.agrios.org.mapper.OrgNodeMapper;
import ai.toafrica.agrios.org.vo.OrgNodeTreeVO;
import ai.toafrica.agrios.system.entity.SysUser;
import ai.toafrica.agrios.system.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sprint 51 -- OrgNode lifecycle + tree shape helpers.
 *
 * <p>All eight ORG decisions land here as service-layer guards:</p>
 *
 * <ul>
 *   <li>#3 -- physical nodes can only flip {@code active}; delete is rejected</li>
 *   <li>#6 -- node type enum + physical-cannot-nest-physical rule</li>
 *   <li>type-specific child rules: TEAM is leaf; PROJECT is leaf; DEPT
 *       cannot hold physical children; GROUP is singleton root</li>
 *   <li>{@code ancestors} is rebuilt server-side on every save -- callers
 *       cannot override it directly</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgNodeService {

    private final OrgNodeMapper nodeMapper;
    private final SysUserMapper userMapper;
    /** Sprint 51 -- Optional: present only after Day 3. Used to invalidate the
     *  subtree-id Redis cache on every mutating call below.
     *  @Lazy proxy breaks the OrgNodeService <-> DataScopeService cycle. */
    @Autowired(required = false)
    @Lazy
    private ai.toafrica.agrios.framework.datascope.DataScopeService dataScopeService;

    /** Physical types that may not nest inside each other (decision #6). */
    private static final Set<String> PHYSICAL_TYPES =
            Set.of("FARM", "PACKHOUSE", "PROCESSING", "WAREHOUSE");

    /** All valid types -- mirrors the DB CHECK constraint in migration 050. */
    private static final Set<String> ALL_TYPES =
            Set.of("GROUP", "FARM", "PACKHOUSE", "PROCESSING", "WAREHOUSE",
                   "DEPT", "TEAM", "PROJECT");

    /** Types that may NOT have children (leaf only). */
    private static final Set<String> LEAF_TYPES = Set.of("TEAM", "PROJECT");

    // -------------------------------------------------------------------
    // Reads
    // -------------------------------------------------------------------

    public List<OrgNode> listAll(boolean includeInactive) {
        LambdaQueryWrapper<OrgNode> q = new LambdaQueryWrapper<OrgNode>()
                .orderByAsc(OrgNode::getDepth)
                .orderByAsc(OrgNode::getSortNo)
                .orderByAsc(OrgNode::getId);
        if (!includeInactive) {
            q.eq(OrgNode::getActive, 1);
        }
        return nodeMapper.selectList(q);
    }

    /** Returns the nested tree shape. Root is GROUP. */
    public List<OrgNodeTreeVO> tree(boolean includeInactive) {
        List<OrgNode> all = listAll(includeInactive);
        // Pre-resolve manager names in one query so we don't N+1 on render
        Map<Long, String> managerNames = resolveManagerNames(all);

        Map<Long, OrgNodeTreeVO> idx = new HashMap<>();
        List<OrgNodeTreeVO> roots = new java.util.ArrayList<>();
        for (OrgNode n : all) {
            OrgNodeTreeVO vo = toTreeVO(n, managerNames);
            idx.put(n.getId(), vo);
        }
        for (OrgNode n : all) {
            OrgNodeTreeVO vo = idx.get(n.getId());
            if (n.getParentId() == null) {
                roots.add(vo);
            } else {
                OrgNodeTreeVO parent = idx.get(n.getParentId());
                if (parent != null) parent.getChildren().add(vo);
            }
        }
        return roots;
    }

    public OrgNode getById(Long id) {
        OrgNode n = nodeMapper.selectById(id);
        if (n == null) throw new BusinessException("Node not found: " + id);
        return n;
    }

    /** Returns the inclusive subtree id set for the given root (cached upstream). */
    public List<Long> subtreeIds(Long rootId) {
        if (rootId == null) return List.of();
        return nodeMapper.selectSubtreeIds(rootId, String.valueOf(rootId));
    }

    // -------------------------------------------------------------------
    // Writes
    // -------------------------------------------------------------------

    @Transactional
    public OrgNode create(OrgNode req) {
        validateType(req.getType());
        validateParent(req.getParentId(), req.getType());
        validateCodeUnique(req.getCode(), null);

        OrgNode parent = req.getParentId() == null ? null : getById(req.getParentId());
        req.setAncestors(buildAncestors(parent));
        req.setDepth(parent == null ? 0 : parent.getDepth() + 1);
        if (req.getActive() == null) req.setActive(1);

        nodeMapper.insert(req);
        invalidateCache(req.getParentId());
        log.info("[org] node created id={} code={} type={} parent={}",
                req.getId(), req.getCode(), req.getType(), req.getParentId());
        return req;
    }

    @Transactional
    public OrgNode update(Long id, OrgNode req) {
        OrgNode existing = getById(id);

        // type cannot be changed (would invalidate child constraints)
        if (req.getType() != null && !req.getType().equals(existing.getType())) {
            throw new BusinessException("Node type cannot be changed after creation");
        }
        // code change is allowed but must remain unique
        if (req.getCode() != null && !req.getCode().equals(existing.getCode())) {
            validateCodeUnique(req.getCode(), id);
            existing.setCode(req.getCode());
        }

        // Parent move: validate target + rebuild ancestors for self and subtree
        if (req.getParentId() != null && !req.getParentId().equals(existing.getParentId())) {
            if (id.equals(req.getParentId())) {
                throw new BusinessException("A node cannot be its own parent");
            }
            validateParent(req.getParentId(), existing.getType());
            OrgNode newParent = getById(req.getParentId());
            // Cycle check -- new parent must not be inside the moving subtree
            if (subtreeIds(id).contains(newParent.getId())) {
                throw new BusinessException("Cannot move a node into its own subtree");
            }
            existing.setParentId(newParent.getId());
            existing.setAncestors(buildAncestors(newParent));
            existing.setDepth(newParent.getDepth() + 1);
            rebuildSubtreeAncestors(existing);
        }

        // Other safe fields
        if (req.getName() != null)        existing.setName(req.getName());
        if (req.getCostCenter() != null)  existing.setCostCenter(req.getCostCenter());
        if (req.getManagerId() != null)   existing.setManagerId(req.getManagerId());
        if (req.getSortNo() != null)      existing.setSortNo(req.getSortNo());
        if (req.getLocation() != null)    existing.setLocation(req.getLocation());
        if (req.getDescription() != null) existing.setDescription(req.getDescription());

        nodeMapper.updateById(existing);
        invalidateCache(id);
        log.info("[org] node updated id={} code={}", id, existing.getCode());
        return existing;
    }

    /** Decision #3 -- physical types may only be deactivated, not deleted. */
    @Transactional
    public void delete(Long id) {
        OrgNode n = getById(id);

        if (PHYSICAL_TYPES.contains(n.getType())) {
            throw new BusinessException(
                    "Physical nodes (FARM/PACKHOUSE/PROCESSING/WAREHOUSE) cannot be deleted. "
                  + "Use deactivate (active=0) instead.");
        }
        if ("GROUP".equals(n.getType())) {
            throw new BusinessException("The root GROUP node cannot be deleted.");
        }
        if (nodeMapper.hasActiveChildren(id)) {
            throw new BusinessException(
                    "Node has active children. Reassign or deactivate them first.");
        }
        nodeMapper.deleteById(id);    // logical delete via @TableLogic
        invalidateCache(id);
        log.info("[org] node soft-deleted id={} type={}", id, n.getType());
    }

    /** Flip active flag -- the only way to retire a physical node. */
    @Transactional
    public OrgNode setActive(Long id, boolean active) {
        OrgNode n = getById(id);
        n.setActive(active ? 1 : 0);
        nodeMapper.updateById(n);
        invalidateCache(id);
        log.info("[org] node id={} active -> {}", id, active);
        return n;
    }

    private void invalidateCache(Long nodeId) {
        if (dataScopeService != null) {
            try { dataScopeService.invalidate(nodeId); } catch (Exception ignored) {}
        }
    }

    // -------------------------------------------------------------------
    // Validators
    // -------------------------------------------------------------------

    private void validateType(String type) {
        if (type == null || !ALL_TYPES.contains(type)) {
            throw new BusinessException("Invalid node type: " + type
                    + ". Allowed: " + ALL_TYPES);
        }
    }

    private void validateParent(Long parentId, String childType) {
        if ("GROUP".equals(childType)) {
            if (parentId != null) {
                throw new BusinessException("GROUP must be root (parent_id must be NULL)");
            }
            // Singleton GROUP check
            Long groupCount = nodeMapper.selectCount(
                    new LambdaQueryWrapper<OrgNode>().eq(OrgNode::getType, "GROUP"));
            if (groupCount > 0) {
                throw new BusinessException("A GROUP node already exists; only one is allowed");
            }
            return;
        }
        if (parentId == null) {
            throw new BusinessException("Non-GROUP nodes must have a parent");
        }
        OrgNode parent = nodeMapper.selectById(parentId);
        if (parent == null) {
            throw new BusinessException("Parent node not found: " + parentId);
        }
        // Leaf parents cannot have children
        if (LEAF_TYPES.contains(parent.getType())) {
            throw new BusinessException("Type " + parent.getType()
                    + " is a leaf and cannot have children");
        }
        // Decision #6 -- physical may not nest physical
        if (PHYSICAL_TYPES.contains(childType) && PHYSICAL_TYPES.contains(parent.getType())) {
            throw new BusinessException(
                    "Physical node " + childType + " cannot nest inside physical " + parent.getType());
        }
        // DEPT can hold dept/team only; cannot hold physical children
        if ("DEPT".equals(parent.getType()) && PHYSICAL_TYPES.contains(childType)) {
            throw new BusinessException(
                    "DEPT cannot contain a physical node ("+ childType +"); place it under GROUP or a FARM");
        }
    }

    private void validateCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<OrgNode> q = new LambdaQueryWrapper<OrgNode>()
                .eq(OrgNode::getCode, code);
        if (excludeId != null) q.ne(OrgNode::getId, excludeId);
        if (nodeMapper.selectCount(q) > 0) {
            throw new BusinessException("Code already exists: " + code);
        }
    }

    // -------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------

    private String buildAncestors(OrgNode parent) {
        if (parent == null) return "";
        String pa = parent.getAncestors();
        if (pa == null || pa.isBlank()) return String.valueOf(parent.getId());
        return pa + "/" + parent.getId();
    }

    private void rebuildSubtreeAncestors(OrgNode movedRoot) {
        List<Long> ids = subtreeIds(movedRoot.getId());
        if (ids.isEmpty()) return;
        // Fetch descendants in BFS order by depth and rebuild
        List<OrgNode> descendants = nodeMapper.selectList(
                new LambdaQueryWrapper<OrgNode>()
                        .in(OrgNode::getId, ids)
                        .ne(OrgNode::getId, movedRoot.getId())
                        .orderByAsc(OrgNode::getDepth)
                        .orderByAsc(OrgNode::getId));
        Map<Long, OrgNode> byId = new HashMap<>();
        byId.put(movedRoot.getId(), movedRoot);
        for (OrgNode d : descendants) {
            OrgNode parent = byId.get(d.getParentId());
            if (parent != null) {
                d.setAncestors(buildAncestors(parent));
                d.setDepth(parent.getDepth() + 1);
                nodeMapper.updateById(d);
                byId.put(d.getId(), d);
            }
        }
    }

    private Map<Long, String> resolveManagerNames(List<OrgNode> nodes) {
        Set<Long> managerIds = new java.util.HashSet<>();
        for (OrgNode n : nodes) if (n.getManagerId() != null) managerIds.add(n.getManagerId());
        if (managerIds.isEmpty()) return Map.of();
        List<SysUser> users = userMapper.selectBatchIds(managerIds);
        Map<Long, String> m = new HashMap<>();
        for (SysUser u : users) {
            String label = u.getNickname() != null ? u.getNickname() : u.getUsername();
            m.put(u.getId(), label);
        }
        return m;
    }

    private OrgNodeTreeVO toTreeVO(OrgNode n, Map<Long, String> managerNames) {
        OrgNodeTreeVO vo = new OrgNodeTreeVO();
        vo.setId(n.getId());
        vo.setParentId(n.getParentId());
        vo.setCode(n.getCode());
        vo.setName(n.getName());
        vo.setType(n.getType());
        vo.setManagerId(n.getManagerId());
        vo.setManagerName(n.getManagerId() == null ? null
                : managerNames.get(n.getManagerId()));
        vo.setLocation(n.getLocation());
        vo.setActive(n.getActive());
        vo.setSortNo(n.getSortNo());
        vo.setDepth(n.getDepth());
        return vo;
    }
}
