package ai.toafrica.agrios.org.service;

import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.org.entity.OrgNodeTag;
import ai.toafrica.agrios.org.entity.OrgTag;
import ai.toafrica.agrios.org.mapper.OrgNodeTagMapper;
import ai.toafrica.agrios.org.mapper.OrgTagMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Sprint 51 -- tag CRUD + node attachment.
 *
 * <p>PRD-ORG-v0.2 § 3.3 rules enforced here: category must be in the
 * allowed set; nesting / hierarchical names are not blocked structurally
 * but are flagged in code review.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgTagService {

    private final OrgTagMapper tagMapper;
    private final OrgNodeTagMapper attachMapper;

    /** Allowed categories. New categories require PRD update + sprint review. */
    private static final Set<String> CATEGORIES =
            Set.of("SEASON", "PROJECT", "COMPLIANCE_ZONE", "CERTIFICATION");

    // -------------------------------------------------------------------
    // Tag CRUD
    // -------------------------------------------------------------------

    public List<OrgTag> listAll(String category, boolean includeInactive) {
        LambdaQueryWrapper<OrgTag> q = new LambdaQueryWrapper<OrgTag>()
                .orderByAsc(OrgTag::getCategory)
                .orderByAsc(OrgTag::getCode);
        if (category != null && !category.isBlank()) {
            q.eq(OrgTag::getCategory, category);
        }
        if (!includeInactive) q.eq(OrgTag::getActive, 1);
        return tagMapper.selectList(q);
    }

    @Transactional
    public OrgTag create(OrgTag req) {
        if (req.getCode() == null || req.getCode().isBlank()) {
            throw new BusinessException("Tag code is required");
        }
        if (req.getCategory() == null || !CATEGORIES.contains(req.getCategory())) {
            throw new BusinessException("Tag category must be one of " + CATEGORIES);
        }
        if (tagMapper.selectCount(
                new LambdaQueryWrapper<OrgTag>().eq(OrgTag::getCode, req.getCode())) > 0) {
            throw new BusinessException("Tag code already exists: " + req.getCode());
        }
        if (req.getActive() == null) req.setActive(1);
        if (req.getCreatedAt() == null) req.setCreatedAt(LocalDateTime.now());
        tagMapper.insert(req);
        return req;
    }

    @Transactional
    public void delete(Long id) {
        // Detach from any nodes first
        attachMapper.delete(
                new LambdaQueryWrapper<OrgNodeTag>().eq(OrgNodeTag::getTagId, id));
        tagMapper.deleteById(id);
        log.info("[org-tag] deleted id={}", id);
    }

    // -------------------------------------------------------------------
    // Attach / detach to nodes
    // -------------------------------------------------------------------

    public List<Long> tagIdsForNode(Long nodeId) {
        return attachMapper.selectList(
                        new LambdaQueryWrapper<OrgNodeTag>().eq(OrgNodeTag::getNodeId, nodeId))
                .stream().map(OrgNodeTag::getTagId).toList();
    }

    public List<Long> nodeIdsForTag(Long tagId) {
        return attachMapper.selectList(
                        new LambdaQueryWrapper<OrgNodeTag>().eq(OrgNodeTag::getTagId, tagId))
                .stream().map(OrgNodeTag::getNodeId).toList();
    }

    @Transactional
    public void attach(Long nodeId, Long tagId) {
        if (attachMapper.selectCount(
                new LambdaQueryWrapper<OrgNodeTag>()
                        .eq(OrgNodeTag::getNodeId, nodeId)
                        .eq(OrgNodeTag::getTagId, tagId)) > 0) {
            return;     // idempotent
        }
        attachMapper.insert(new OrgNodeTag(nodeId, tagId, LocalDateTime.now()));
        log.info("[org-tag] attached node={} tag={}", nodeId, tagId);
    }

    @Transactional
    public void detach(Long nodeId, Long tagId) {
        attachMapper.delete(
                new LambdaQueryWrapper<OrgNodeTag>()
                        .eq(OrgNodeTag::getNodeId, nodeId)
                        .eq(OrgNodeTag::getTagId, tagId));
        log.info("[org-tag] detached node={} tag={}", nodeId, tagId);
    }
}
