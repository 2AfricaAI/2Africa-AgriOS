package ai.toafrica.agrios.master.service;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.master.mapper.InputStockLogMapper;
import ai.toafrica.agrios.master.vo.InputStockLogVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 出入库流水查询 (Sprint 22.3) — 只读, 无写接口
 */
@Service
@RequiredArgsConstructor
public class InputStockLogService {

    private final InputStockLogMapper logMapper;

    public PageResult<InputStockLogVO> page(Long inputItemId, Long warehouseId,
                                            String direction, String reasonType,
                                            PageQuery pq) {
        QueryWrapper<InputStockLogVO> q = new QueryWrapper<>();
        if (inputItemId != null) q.eq("l.input_item_id", inputItemId);
        if (warehouseId != null) q.eq("l.warehouse_id", warehouseId);
        if (direction != null && !direction.isBlank()) q.eq("l.direction", direction.trim().toUpperCase());
        if (reasonType != null && !reasonType.isBlank()) q.eq("l.reason_type", reasonType.trim());
        q.orderByDesc("l.created_at");

        Page<InputStockLogVO> p = new Page<>(pq.getPage(), pq.getSize());
        return PageResult.of(logMapper.pageWithJoin(p, q));
    }
}
