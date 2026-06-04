package ai.toafrica.agrios.org.service;

import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.org.entity.OrgUser;
import ai.toafrica.agrios.org.mapper.OrgUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Sprint 51 -- user-to-node membership lifecycle.
 *
 * <p>Decision #4 -- payroll attribution relies on time-bounded rows.
 * When an employee is reassigned, the active row is CLOSED (effective_to
 * = today) and a NEW row is INSERTED for the new node.
 * History rows are never deleted.</p>
 *
 * <p>Decision #2 -- exactly one is_primary row per user globally is
 * enforced here.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgUserService {

    private final OrgUserMapper orgUserMapper;

    public List<OrgUser> listByUser(Long userId, boolean includeHistory) {
        LambdaQueryWrapper<OrgUser> q = new LambdaQueryWrapper<OrgUser>()
                .eq(OrgUser::getUserId, userId)
                .orderByDesc(OrgUser::getEffectiveFrom);
        if (!includeHistory) {
            q.and(w -> w.isNull(OrgUser::getEffectiveTo)
                       .or().ge(OrgUser::getEffectiveTo, LocalDate.now()));
        }
        return orgUserMapper.selectList(q);
    }

    public List<OrgUser> listByNode(Long nodeId, boolean activeOnly) {
        LambdaQueryWrapper<OrgUser> q = new LambdaQueryWrapper<OrgUser>()
                .eq(OrgUser::getNodeId, nodeId)
                .orderByDesc(OrgUser::getEffectiveFrom);
        if (activeOnly) {
            q.and(w -> w.isNull(OrgUser::getEffectiveTo)
                       .or().ge(OrgUser::getEffectiveTo, LocalDate.now()));
        }
        return orgUserMapper.selectList(q);
    }

    /**
     * Returns the user's currently-active primary node id, or {@code null}
     * if they have none. Used heavily by the {@code @DataScope} interceptor.
     */
    public Long currentPrimaryNodeId(Long userId) {
        OrgUser row = orgUserMapper.selectOne(
                new LambdaQueryWrapper<OrgUser>()
                        .eq(OrgUser::getUserId, userId)
                        .eq(OrgUser::getIsPrimary, 1)
                        .and(w -> w.isNull(OrgUser::getEffectiveTo)
                                   .or().ge(OrgUser::getEffectiveTo, LocalDate.now()))
                        .last("LIMIT 1"));
        return row == null ? null : row.getNodeId();
    }

    /** Add a new membership row. If is_primary=1, the prior primary is closed. */
    @Transactional
    public OrgUser assign(OrgUser req) {
        if (req.getUserId() == null || req.getNodeId() == null) {
            throw new BusinessException("userId and nodeId are required");
        }
        if (req.getEffectiveFrom() == null) {
            req.setEffectiveFrom(LocalDate.now());
        }
        if (req.getEffectiveTo() != null
                && req.getEffectiveTo().isBefore(req.getEffectiveFrom())) {
            throw new BusinessException("effective_to cannot be before effective_from");
        }

        // If marking primary, close any existing primary first
        if (req.getIsPrimary() != null && req.getIsPrimary() == 1) {
            closeExistingPrimary(req.getUserId(), req.getEffectiveFrom());
        } else if (req.getIsPrimary() == null) {
            req.setIsPrimary(0);
        }
        if (req.getIsManager() == null) req.setIsManager(0);

        orgUserMapper.insert(req);
        log.info("[org-user] assigned user={} -> node={} primary={} manager={} from={}",
                req.getUserId(), req.getNodeId(),
                req.getIsPrimary(), req.getIsManager(), req.getEffectiveFrom());
        return req;
    }

    /**
     * End an active membership today. Used when an employee leaves a
     * node (transferred or terminated).
     */
    @Transactional
    public void close(Long id, LocalDate effectiveTo) {
        OrgUser row = orgUserMapper.selectById(id);
        if (row == null) throw new BusinessException("Org-user row not found: " + id);
        if (row.getEffectiveTo() != null) {
            throw new BusinessException("Row is already closed at " + row.getEffectiveTo());
        }
        LocalDate end = effectiveTo == null ? LocalDate.now() : effectiveTo;
        if (end.isBefore(row.getEffectiveFrom())) {
            throw new BusinessException("effective_to cannot be before effective_from");
        }
        row.setEffectiveTo(end);
        orgUserMapper.updateById(row);
        log.info("[org-user] closed id={} to={}", id, end);
    }

    private void closeExistingPrimary(Long userId, LocalDate newFrom) {
        OrgUser existing = orgUserMapper.selectOne(
                new LambdaQueryWrapper<OrgUser>()
                        .eq(OrgUser::getUserId, userId)
                        .eq(OrgUser::getIsPrimary, 1)
                        .isNull(OrgUser::getEffectiveTo)
                        .last("LIMIT 1"));
        if (existing == null) return;
        // Close the day before the new one starts; if same day, set to today
        LocalDate end = newFrom.isAfter(existing.getEffectiveFrom())
                ? newFrom.minusDays(1)
                : LocalDate.now();
        existing.setEffectiveTo(end);
        orgUserMapper.updateById(existing);
        log.info("[org-user] closed prior primary id={} to={}", existing.getId(), end);
    }
}
