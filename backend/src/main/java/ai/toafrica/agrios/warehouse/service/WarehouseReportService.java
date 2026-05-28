package ai.toafrica.agrios.warehouse.service;

import ai.toafrica.agrios.warehouse.mapper.WarehouseReportMapper;
import ai.toafrica.agrios.warehouse.vo.WarehouseReportVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WarehouseReportService {

    private final WarehouseReportMapper reportMapper;

    /**
     * 聚合仓库作业报表
     * @param from inclusive (default = today - 30)
     * @param to   exclusive (default = today + 1, i.e. include today)
     */
    public WarehouseReportVO report(LocalDate from, LocalDate to) {
        if (from == null) from = LocalDate.now().minusDays(30);
        if (to == null) to = LocalDate.now().plusDays(1);

        WarehouseReportVO vo = new WarehouseReportVO();

        // 1) doc counts: rotate rows into nested map
        Map<String, Map<String, Integer>> counts = new HashMap<>();
        for (Map<String, Object> row : reportMapper.docCountRows(from, to)) {
            String docType = (String) row.get("doc_type");
            String status  = (String) row.get("status");
            Number cnt     = (Number) row.get("cnt");
            counts.computeIfAbsent(docType, k -> new HashMap<>()).put(status, cnt.intValue());
        }
        vo.setDocCounts(counts);

        // 2) top inbound / outbound
        vo.setTopInbound(reportMapper.topMovement("IN", from, to, 10));
        vo.setTopOutbound(reportMapper.topMovement("OUT", from, to, 10));

        // 3) low stock
        vo.setLowStock(reportMapper.lowStockItems());

        // 4) stock by warehouse
        vo.setStockByWarehouse(reportMapper.stockByWarehouse());

        return vo;
    }
}
