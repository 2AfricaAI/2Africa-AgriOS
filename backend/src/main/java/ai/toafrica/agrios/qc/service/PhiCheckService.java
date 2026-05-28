package ai.toafrica.agrios.qc.service;

import ai.toafrica.agrios.qc.mapper.PhiCheckMapper;
import ai.toafrica.agrios.qc.vo.PhiCheckVO;
import ai.toafrica.agrios.qc.vo.PhiCheckVO.BlockingSpray;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * PHI (Pre-Harvest Interval) check service - Sprint 23 / Phase 5.
 *
 * 给定一个 plan 和拟采收日期,计算:
 *   - earliestSafeDate = max(sprayDate + phiDays) across all recent sprays
 *   - blocked = proposedDate < earliestSafeDate
 *
 * 业务用法:
 *   1. HarvestRecordService.create() 前调 → 阻断早采
 *   2. 决策中心 PhiBlockRule.evaluate() 调 → 生成预警
 *   3. 前端创建采收单时实时调 → 提示
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PhiCheckService {

    /** 只看近 90 天的喷药 (足够覆盖最长的 PHI) */
    private static final int LOOKBACK_DAYS = 90;

    private final PhiCheckMapper phiMapper;

    /**
     * 检查 plan 在 proposedDate 采收是否违反 PHI.
     *
     * @param planId         种植计划 ID
     * @param proposedDate   拟采收日期 (null = today)
     */
    public PhiCheckVO checkPlan(Long planId, LocalDate proposedDate) {
        if (proposedDate == null) proposedDate = LocalDate.now();
        LocalDate since = proposedDate.minusDays(LOOKBACK_DAYS);

        List<BlockingSpray> sprays = phiMapper.findRecentSprays(planId, since);

        PhiCheckVO vo = new PhiCheckVO();
        vo.setPlanId(planId);
        vo.setProposedDate(proposedDate);

        // 求最晚的 safe_after = max(spray_date + phi_days)
        LocalDate earliestSafe = null;
        List<BlockingSpray> blocking = new ArrayList<>();
        for (BlockingSpray s : sprays) {
            if (s.getSafeAfter() == null) continue;
            // 如果 proposedDate < safe_after → 这次喷药构成阻断
            if (proposedDate.isBefore(s.getSafeAfter())) {
                blocking.add(s);
            }
            if (earliestSafe == null || s.getSafeAfter().isAfter(earliestSafe)) {
                earliestSafe = s.getSafeAfter();
            }
        }

        vo.setEarliestSafeDate(earliestSafe);
        vo.setBlocked(!blocking.isEmpty());
        vo.setBlockingSprays(blocking);
        LocalDate today = LocalDate.now();
        if (earliestSafe != null && earliestSafe.isAfter(today)) {
            vo.setDaysRemaining((int) ChronoUnit.DAYS.between(today, earliestSafe));
        } else {
            vo.setDaysRemaining(0);
        }
        return vo;
    }

    /**
     * 同上,按 plot 查(跨 plan 的农药残留)
     */
    public PhiCheckVO checkPlot(Long plotId, LocalDate proposedDate) {
        if (proposedDate == null) proposedDate = LocalDate.now();
        LocalDate since = proposedDate.minusDays(LOOKBACK_DAYS);
        List<BlockingSpray> sprays = phiMapper.findRecentSpraysByPlot(plotId, since);

        PhiCheckVO vo = new PhiCheckVO();
        vo.setProposedDate(proposedDate);
        LocalDate earliestSafe = null;
        List<BlockingSpray> blocking = new ArrayList<>();
        for (BlockingSpray s : sprays) {
            if (s.getSafeAfter() == null) continue;
            if (proposedDate.isBefore(s.getSafeAfter())) blocking.add(s);
            if (earliestSafe == null || s.getSafeAfter().isAfter(earliestSafe)) earliestSafe = s.getSafeAfter();
        }
        vo.setEarliestSafeDate(earliestSafe);
        vo.setBlocked(!blocking.isEmpty());
        vo.setBlockingSprays(blocking);
        LocalDate today = LocalDate.now();
        if (earliestSafe != null && earliestSafe.isAfter(today)) {
            vo.setDaysRemaining((int) ChronoUnit.DAYS.between(today, earliestSafe));
        } else {
            vo.setDaysRemaining(0);
        }
        return vo;
    }
}
