package ai.toafrica.agrios.ops.service;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.framework.security.SecurityUtil;
import ai.toafrica.agrios.ops.entity.ActionItem;
import ai.toafrica.agrios.ops.mapper.ActionItemMapper;
import ai.toafrica.agrios.ops.vo.ActionItemVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * action_item 业务服务 — 列表查询 + 用户标记完成/忽略.
 *
 * 规则引擎写入逻辑见 {@link ActionEngineService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActionItemService {

    private final ActionItemMapper mapper;

    /**
     * 列表查询.
     *
     * 默认只看 status=open 的; 但若调用方显式传 status 则按传入过滤.
     * 排序: severity (high > medium > low) → due_date 升序 → id 降序.
     */
    public PageResult<ActionItemVO> page(String category, String ownerRole, String status,
                                         String severity, PageQuery pq) {
        LambdaQueryWrapper<ActionItem> q = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            q.eq(ActionItem::getStatus, status.trim());
        } else {
            q.eq(ActionItem::getStatus, "open");
        }
        if (category != null && !category.isBlank())  q.eq(ActionItem::getCategory,  category.trim());
        if (ownerRole != null && !ownerRole.isBlank()) q.eq(ActionItem::getOwnerRole, ownerRole.trim());
        if (severity != null && !severity.isBlank())   q.eq(ActionItem::getSeverity,  severity.trim());

        // MySQL 没有原生 enum-order, 用 FIELD 自定义排序
        q.last("ORDER BY FIELD(severity,'high','medium','low'), due_date ASC, id DESC");

        Page<ActionItem> p = new Page<>(pq.getPage(), pq.getSize());
        return PageResult.of(mapper.selectPage(p, q), this::toVO);
    }

    /** 标记完成 */
    public void markDone(Long id, String remark) {
        ActionItem a = mapper.selectById(id);
        if (a == null) throw new BusinessException(R.NOT_FOUND, "Action not found");
        if (!"open".equals(a.getStatus())) {
            throw new BusinessException(
                "Only open actions can be marked done (current: " + a.getStatus() + ")");
        }
        a.setStatus("done");
        a.setResolvedAt(LocalDateTime.now());
        a.setResolvedBy(SecurityUtil.currentUserId());
        a.setResolvedRemark(remark);
        mapper.updateById(a);
        log.info("[Action {} done] rule={} ref={}/{} by user={}",
                a.getId(), a.getRuleCode(), a.getRefType(), a.getRefId(), a.getResolvedBy());
    }

    /** 忽略 */
    public void dismiss(Long id, String remark) {
        ActionItem a = mapper.selectById(id);
        if (a == null) throw new BusinessException(R.NOT_FOUND, "Action not found");
        if (!"open".equals(a.getStatus())) {
            throw new BusinessException(
                "Only open actions can be dismissed (current: " + a.getStatus() + ")");
        }
        a.setStatus("dismissed");
        a.setResolvedAt(LocalDateTime.now());
        a.setResolvedBy(SecurityUtil.currentUserId());
        a.setResolvedRemark(remark);
        mapper.updateById(a);
        log.info("[Action {} dismissed] rule={} ref={}/{} by user={}",
                a.getId(), a.getRuleCode(), a.getRefType(), a.getRefId(), a.getResolvedBy());
    }

    private ActionItemVO toVO(ActionItem a) {
        ActionItemVO v = new ActionItemVO();
        BeanUtils.copyProperties(a, v);
        return v;
    }
}
