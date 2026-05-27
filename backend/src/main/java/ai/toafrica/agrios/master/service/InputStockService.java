package ai.toafrica.agrios.master.service;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.master.entity.InputStock;
import ai.toafrica.agrios.master.entity.InputStockLog;
import ai.toafrica.agrios.master.mapper.InputStockLogMapper;
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
    private final InputStockLogMapper logMapper;

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
    /**
     * 调整库存 + 自动写流水日志 (Sprint 22.3 升级)
     *
     * @param inputItemId  物料 ID
     * @param warehouseId  仓库 ID
     * @param delta        变动量 (正=入库, 负=出库)
     * @param reasonType   po_receive / activity_consume / stocktake_adjust / damage / manual / ...
     * @param referenceType 多态引用类型 (purchase_order / activity / null)
     * @param referenceId   多态引用 ID (PO id / Activity id / null)
     * @param operatorId   操作人 (sys_user.id, 可 null)
     * @param remark       备注
     */
    @Transactional(rollbackFor = Exception.class)
    public void adjustStock(Long inputItemId, Long warehouseId, BigDecimal delta,
                            String reasonType, String referenceType, Long referenceId,
                            Long operatorId, String remark) {
        InputStock stock = findOrCreate(inputItemId, warehouseId);
        stock.setQtyOnHand(stock.getQtyOnHand().add(delta));
        stock.setLastStockAt(LocalDateTime.now());
        stockMapper.updateById(stock);

        // 写流水日志 (Sprint 22.3)
        InputStockLog entry = new InputStockLog();
        entry.setInputItemId(inputItemId);
        entry.setWarehouseId(warehouseId);
        entry.setDirection(delta.signum() >= 0 ? "IN" : "OUT");
        entry.setQty(delta.abs());
        entry.setReasonType(reasonType);
        entry.setReferenceType(referenceType);
        entry.setReferenceId(referenceId);
        entry.setQtyAfter(stock.getQtyOnHand());
        entry.setOperatorId(operatorId);
        entry.setRemark(remark);
        logMapper.insert(entry);

        log.info("[InputStock] item={} wh={} delta={} reason={} ref={}:{} -> onHand={}",
                inputItemId, warehouseId, delta.toPlainString(), reasonType,
                referenceType, referenceId, stock.getQtyOnHand().toPlainString());
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
