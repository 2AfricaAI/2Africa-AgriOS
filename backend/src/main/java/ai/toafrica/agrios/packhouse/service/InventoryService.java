package ai.toafrica.agrios.packhouse.service;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.packhouse.mapper.InventoryMapper;
import ai.toafrica.agrios.packhouse.vo.InventoryRow;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryMapper inventoryMapper;

    public PageResult<InventoryRow> page(Long skuId, Long batchId, Long locationId,
                                          String grade, String status, PageQuery pq) {
        QueryWrapper<InventoryRow> q = new QueryWrapper<>();
        if (skuId != null) q.eq("i.sku_id", skuId);
        if (batchId != null) q.eq("i.batch_id", batchId);
        if (locationId != null) q.eq("i.location_id", locationId);
        if (grade != null && !grade.isBlank()) q.eq("i.grade", grade.trim());
        if (status != null && !status.isBlank()) q.eq("i.status", status.trim());
        q.orderByDesc("i.updated_at").orderByDesc("i.id");
        Page<InventoryRow> p = new Page<>(pq.getPage(), pq.getSize());
        return PageResult.of(inventoryMapper.pageWithJoin(p, q));
    }
}
