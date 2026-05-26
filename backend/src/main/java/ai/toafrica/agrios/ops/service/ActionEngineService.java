package ai.toafrica.agrios.ops.service;

import ai.toafrica.agrios.ops.entity.ActionItem;
import ai.toafrica.agrios.ops.mapper.ActionItemMapper;
import ai.toafrica.agrios.ops.service.rule.ActionRule;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 规则引擎 - Sprint 10 决策中心核心.
 *
 * refresh() 流程:
 *   1. 收集 Spring 注入的所有 ActionRule (Bean), 逐条 evaluate()
 *   2. 对每条返回的 ActionItem:
 *        - 按 (rule_code, ref_type, ref_id) 查 db
 *        - 有就 update (status → open, 复写 title/description/data_snapshot 等), 让"过期"项重新亮灯
 *        - 无就 insert (status = open)
 *   3. 把数据库里 status=open 但本轮没再被任何规则命中的 → 标 auto_resolved
 *   4. 返回本次触发的总条数
 *
 * 注: 整个 refresh() 在一个事务里, 写库失败会回滚, 不会出现"一半数据".
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActionEngineService {

    /** Spring 自动注入所有实现 ActionRule 接口的 Bean */
    private final List<ActionRule> rules;
    private final ActionItemMapper mapper;

    @Transactional(rollbackFor = Exception.class)
    public int refresh() {
        log.info("[ActionEngine] refresh start, rules registered = {}", rules.size());

        // 本轮命中的 (rule_code, ref_type, ref_id) 三元组集合
        Set<String> hitKeys = new HashSet<>();
        int totalHits = 0;

        for (ActionRule rule : rules) {
            List<ActionItem> items;
            try {
                items = rule.evaluate();
            } catch (Exception e) {
                log.error("[ActionEngine] rule {} evaluate failed", rule.ruleCode(), e);
                continue;
            }
            if (items == null || items.isEmpty()) {
                log.info("[ActionEngine] rule {} → 0 hits", rule.ruleCode());
                continue;
            }
            for (ActionItem hit : items) {
                upsertOne(hit);
                hitKeys.add(keyOf(hit.getRuleCode(), hit.getRefType(), hit.getRefId()));
                totalHits++;
            }
            log.info("[ActionEngine] rule {} → {} hits", rule.ruleCode(), items.size());
        }

        // 把之前 open 但本轮没再触发的, 自动 resolve
        int autoResolved = autoResolveStale(hitKeys);
        log.info("[ActionEngine] refresh done, hits={}, auto_resolved={}", totalHits, autoResolved);
        return totalHits;
    }

    // ============================================================
    // upsert 单条 (rule_code + ref_type + ref_id 三元组唯一)
    // ============================================================
    private void upsertOne(ActionItem hit) {
        LambdaQueryWrapper<ActionItem> q = new LambdaQueryWrapper<ActionItem>()
                .eq(ActionItem::getRuleCode, hit.getRuleCode())
                .eq(ActionItem::getRefType,  hit.getRefType())
                .eq(ActionItem::getRefId,    hit.getRefId());
        ActionItem existing = mapper.selectOne(q);

        if (existing == null) {
            hit.setStatus("open");
            mapper.insert(hit);
            return;
        }

        // 复用 id, 更新业务字段, 把之前 done/dismissed/auto_resolved 的拉回 open
        existing.setSeverity(hit.getSeverity());
        existing.setCategory(hit.getCategory());
        existing.setTitle(hit.getTitle());
        existing.setDescription(hit.getDescription());
        existing.setOwnerRole(hit.getOwnerRole());
        existing.setRefCode(hit.getRefCode());
        existing.setDueDate(hit.getDueDate());
        existing.setDataSnapshot(hit.getDataSnapshot());
        existing.setStatus("open");
        existing.setResolvedAt(null);
        existing.setResolvedBy(null);
        existing.setResolvedRemark(null);
        mapper.updateById(existing);
    }

    // ============================================================
    // 把 status=open 但本轮没命中的 → auto_resolved
    // ============================================================
    private int autoResolveStale(Set<String> hitKeys) {
        List<ActionItem> openItems = mapper.selectList(
                new LambdaQueryWrapper<ActionItem>().eq(ActionItem::getStatus, "open"));

        int n = 0;
        LocalDateTime now = LocalDateTime.now();
        for (ActionItem it : openItems) {
            if (hitKeys.contains(keyOf(it.getRuleCode(), it.getRefType(), it.getRefId()))) continue;
            it.setStatus("auto_resolved");
            it.setResolvedAt(now);
            it.setResolvedRemark("Auto-resolved: rule no longer triggers");
            mapper.updateById(it);
            n++;
        }
        return n;
    }

    private static String keyOf(String ruleCode, String refType, Long refId) {
        return ruleCode + "|" + refType + "|" + refId;
    }
}
