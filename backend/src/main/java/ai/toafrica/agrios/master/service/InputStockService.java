package ai.toafrica.agrios.master.service;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.master.entity.InputStock;
import ai.toafrica.agrios.master.mapper.InputStockMapper;
import ai.toafrica.agrios.master.vo.InputStockVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 投入品库存服务 (Sprint 22.2)
 *
 * 当前只提供只读查询 + 内部 adjustStock 方法 (供 22.4 PO 收货 / 22.5 Activity 出库调用)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InputStockService {

    private final InputStockMapper stockMapper;

    // ============================================================
    // 列表查询 (分页 + 多维过滤)
    // ============================================================
    public PageResult<InputStockVO> page(Long inputItemId, Long warehouseId,
                                         String inputType, Boolean lowStockOnly,
                                         PageQuery pq) {
        QueryWrapper<InputStockVO> q = new QueryWrapper<>();
        if (inputItemId != null)                       q.eq("s.input_item_id", inputItemId);
        if (warehouseId != null)                       q.eq("s.warehouse_id", warehouseId);
        if (inputType != null && !inputType.isBlank()) q.eq("ii.input_type", inputType.trim());
        if (Boolean.TRUE.equals(lowStockOnly)) {
            // 低库存: on_hand - reserved < input_item.min_stock_qty (且 min_stock_qty IS NOT NULL)
            q.apply("ii.min_stock_qty IS NOT NULL AND (s.qty_on_hand - s.qty_reserved) < ii.min_stock_qty");
        }
        q.orderByDesc("s.last_stock_at");

        Page<InputStockVO> p = new Page<>(pq.getPage(), pq.getSize());
        return PageResult.of(stockMapper.pageWithJoin(p, q));
    }

    // ============================================================
    // 内部: 调整库存 (入库正值, 出库负值)
    //   Sprint 22.4 (PO receive) 和 22.5 (Activity consume) 会调
    // ============================================================
    @Transactional(rollbackFor = Exception.class)
    public void adjustStock(Long inputItemId, Long warehouseId, BigDecimal delta, String reason) {
        InputStock stock = findOrCreate(inputItemId, warehouseId);
        stock.setQtyOnHand(stock.getQtyOnHand().add(delta));
        stock.setLastStockAt(LocalDateTime.now());
        stockMapper.updateById(stock);
        log.info("[InputStock] item={} warehouse={} delta={} reason={} -> onHand={}",
                inputItemId, warehouseId, delta.toPlainString(), reason,
                stock.getQtyOnHand().toPlainString());
    }

    /**
     * 获取或自动创建库存行 (UPSERT 语义)
     */
    private InputStock findOrCreate(Long inputItemId, Long warehouseId) {
        LambdaQueryWrapper<InputStock> q = new LambdaQueryWrapper<>();
        q.eq(InputStock::getInputItemId, inputItemId)
         .eq(InputStock::getWarehouseId, warehouseId);
        InputStock stock = stockMapper.selectOne(q);
        if (stock == null) {
            stock = new InputStock();
            stock.setInputItemId(inputItemId);
            stock.setWarehouseId(warehouseId);
            stock.setQtyOnHand(BigDecimal.ZERO);
            stock.setQtyReserved(BigDecimal.ZERO);
            stock.setLastStockAt(LocalDateTime.now());
            stockMapper.insert(stock);
            log.info("[InputStock] created new row: item={} warehouse={}", inputItemId, warehouseId);
        }
        return stock;
    }
}
