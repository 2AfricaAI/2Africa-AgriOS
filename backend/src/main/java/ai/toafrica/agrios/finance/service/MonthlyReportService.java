package ai.toafrica.agrios.finance.service;

import ai.toafrica.agrios.finance.mapper.MonthlyReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 月度经营报表 - Sprint 15.
 *   V2.0 文档承诺: "每月 5 日前能出上月经营报表"
 *   现阶段提供过去 12 个月数据, 前端做月份切片/Excel 导出.
 */
@Service
@RequiredArgsConstructor
public class MonthlyReportService {

    private final MonthlyReportMapper mapper;

    public List<Map<String, Object>> getMonthlySummary() {
        return mapper.monthlySummary();
    }
}
