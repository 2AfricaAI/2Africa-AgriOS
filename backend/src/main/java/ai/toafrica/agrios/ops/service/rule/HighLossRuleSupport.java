package ai.toafrica.agrios.ops.service.rule;

import lombok.RequiredArgsConstructor;
import org.apache.ibatis.annotations.Param;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 把 R-INV-02 规则需要的"按 harvest_record_id 聚合 batch.qty_kg"
 * 抽成单独的 Spring Bean, 避免污染 BatchMapper.
 *
 * 用 JdbcTemplate 直接跑动态 IN 查询 (避免 mybatis-plus IN 类型推断问题).
 */
@Component
@RequiredArgsConstructor
public class HighLossRuleSupport implements HighLossRule.HarvestBatchSumProvider {

    private final JdbcTemplate jdbc;

    @Override
    public Map<Long, BigDecimal> sumQtyByHarvestRecord(@Param("ids") List<Long> harvestIds) {
        Map<Long, BigDecimal> out = new HashMap<>();
        if (harvestIds == null || harvestIds.isEmpty()) return out;

        // 用 ?,?,?... 占位, 安全防注入
        String placeholders = harvestIds.stream().map(x -> "?").collect(Collectors.joining(","));
        String sql = "SELECT harvest_record_id, COALESCE(SUM(qty_kg), 0) AS qty "
                   + "FROM batch WHERE harvest_record_id IN (" + placeholders + ") "
                   + "  AND deleted_at IS NULL "
                   + "GROUP BY harvest_record_id";

        jdbc.query(sql, (rs) -> {
            long hid = rs.getLong("harvest_record_id");
            BigDecimal qty = rs.getBigDecimal("qty");
            if (qty == null) qty = BigDecimal.ZERO;
            out.put(hid, qty);
        }, harvestIds.toArray());

        return out;
    }
}
