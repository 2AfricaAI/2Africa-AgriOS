package ai.toafrica.agrios.production.service;

import ai.toafrica.agrios.production.mapper.DashboardMapper;
import ai.toafrica.agrios.production.vo.DashboardSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardMapper dashboardMapper;

    public DashboardSummaryVO summary() {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(6);  // 含今天共 7 天

        DashboardSummaryVO v = new DashboardSummaryVO();
        v.setActivePlanCount(dashboardMapper.countActivePlans());
        v.setPendingActivityCount(dashboardMapper.countPendingActivities());
        v.setTodayHarvestKg(dashboardMapper.sumHarvestByDate(today));
        v.setPendingBatchCount(dashboardMapper.countPendingBatches());

        // 近 7 天采收: 补齐空缺日期为 0,前端画图不用单独处理
        v.setHarvest7Days(fill7Days(from, today, dashboardMapper.harvestByDateRange(from, today)));

        v.setHarvestByCrop(dashboardMapper.harvestByCrop());
        v.setPendingActivities(dashboardMapper.topPendingActivities());
        v.setRecentHarvests(dashboardMapper.topRecentHarvests());
        return v;
    }

    /**
     * DB 只返回有采收记录的日期。补齐 7 天序列(无记录的日期 qty=0)。
     */
    private List<Map<String, Object>> fill7Days(LocalDate from, LocalDate to,
                                                 List<Map<String, Object>> dbRows) {
        Map<String, Object> byDate = new HashMap<>();
        for (Map<String, Object> r : dbRows) {
            // d 可能是 java.sql.Date 或 LocalDate, 统一转 String
            Object dObj = r.get("d");
            String dStr = dObj == null ? "" : dObj.toString();
            byDate.put(dStr, r.get("qty"));
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            String key = d.toString();  // YYYY-MM-DD
            Map<String, Object> row = new HashMap<>();
            row.put("date", key);
            Object q = byDate.get(key);
            row.put("qty", q != null ? q : BigDecimal.ZERO);
            out.add(row);
        }
        return out;
    }
}
