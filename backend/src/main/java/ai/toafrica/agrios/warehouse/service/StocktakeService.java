package ai.toafrica.agrios.warehouse.service;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.master.entity.InputStock;
import ai.toafrica.agrios.master.mapper.InputStockMapper;
import ai.toafrica.agrios.master.service.InputStockService;
import ai.toafrica.agrios.warehouse.entity.WarehouseStocktake;
import ai.toafrica.agrios.warehouse.entity.WarehouseStocktakeItem;
import ai.toafrica.agrios.warehouse.mapper.WarehouseStocktakeItemMapper;
import ai.toafrica.agrios.warehouse.mapper.WarehouseStocktakeMapper;
import ai.toafrica.agrios.warehouse.vo.StocktakeDetailVO;
import ai.toafrica.agrios.warehouse.vo.StocktakeItemVO;
import ai.toafrica.agrios.warehouse.vo.StocktakeVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StocktakeService {

    private final WarehouseStocktakeMapper stocktakeMapper;
    private final WarehouseStocktakeItemMapper itemMapper;
    private final InputStockMapper inputStockMapper;
    private final InputStockService stockService;

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    public PageResult<StocktakeVO> page(String status, Long warehouseId, String countType, PageQuery pq) {
        QueryWrapper<StocktakeVO> q = new QueryWrapper<>();
        if (status != null && !status.isBlank()) q.eq("s.status", status.trim());
        if (warehouseId != null) q.eq("s.warehouse_id", warehouseId);
        if (countType != null && !countType.isBlank()) q.eq("s.count_type", countType.trim());
        q.orderByDesc("s.created_at");
        Page<StocktakeVO> p = new Page<>(pq.getPage(), pq.getSize());
        return PageResult.of(stocktakeMapper.pageWithJoin(p, q));
    }

    public StocktakeDetailVO detail(Long id) {
        QueryWrapper<StocktakeVO> q = new QueryWrapper<>();
        q.eq("s.id", id);
        var records = stocktakeMapper.pageWithJoin(new Page<>(1, 1), q).getRecords();
        if (records.isEmpty()) throw new BusinessException(R.NOT_FOUND, "Stocktake not found");
        StocktakeDetailVO vo = new StocktakeDetailVO();
        vo.setHeader(records.get(0));
        vo.setItems(itemMapper.findByStocktakeId(id));
        return vo;
    }

    /**
     * 创建盘点单 → 自动拍快照 (把该仓库所有 input_stock 的 qty_on_hand 抄到 system_qty)
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long warehouseId, String countType, String remark) {
        String today = LocalDateTime.now().format(YMD);
        int seq = stocktakeMapper.countByDate(today) + 1;
        String code = String.format("ST-%s-%04d", today, seq);

        WarehouseStocktake st = new WarehouseStocktake();
        st.setCode(code);
        st.setWarehouseId(warehouseId);
        st.setCountType(countType != null ? countType : "full");
        st.setStatus("draft");
        st.setRemark(remark);
        stocktakeMapper.insert(st);

        // 拍快照: 查该仓库所有库存行
        List<InputStock> stocks = inputStockMapper.selectList(
                new LambdaQueryWrapper<InputStock>().eq(InputStock::getWarehouseId, warehouseId));
        for (InputStock s : stocks) {
            WarehouseStocktakeItem item = new WarehouseStocktakeItem();
            item.setStocktakeId(st.getId());
            item.setInputItemId(s.getInputItemId());
            item.setSystemQty(s.getQtyOnHand());
            itemMapper.insert(item);
        }
        log.info("[Stocktake created] code={} warehouse={} items={}", code, warehouseId, stocks.size());
        return st.getId();
    }

    /**
     * 提交盘点数 (counting 阶段)
     */
    @Transactional(rollbackFor = Exception.class)
    public void submitCounts(Long id, List<CountEntry> counts, Long counterId) {
        WarehouseStocktake st = stocktakeMapper.selectById(id);
        if (st == null) throw new BusinessException(R.NOT_FOUND, "Stocktake not found");
        if (!"draft".equals(st.getStatus()) && !"counting".equals(st.getStatus()))
            throw new BusinessException("Cannot submit counts in status: " + st.getStatus());

        for (CountEntry ce : counts) {
            WarehouseStocktakeItem item = itemMapper.selectById(ce.itemId);
            if (item == null || !item.getStocktakeId().equals(id)) continue;
            item.setCountQty(ce.countQty);
            item.setRemark(ce.remark);
            itemMapper.updateById(item);
        }
        st.setStatus("counting");
        st.setCountedBy(counterId);
        st.setCountedAt(LocalDateTime.now());
        stocktakeMapper.updateById(st);
        log.info("[Stocktake counted] code={}", st.getCode());
    }

    public record CountEntry(Long itemId, BigDecimal countQty, String remark) {}

    /**
     * 确认盘点 → 计算差异 → adjustStock(diff) + log
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long id, Long operatorId) {
        WarehouseStocktake st = stocktakeMapper.selectById(id);
        if (st == null) throw new BusinessException(R.NOT_FOUND, "Stocktake not found");
        if (!"counting".equals(st.getStatus()))
            throw new BusinessException("Only counting stocktake can be confirmed");

        List<WarehouseStocktakeItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<WarehouseStocktakeItem>().eq(WarehouseStocktakeItem::getStocktakeId, id));

        for (WarehouseStocktakeItem item : items) {
            if (item.getCountQty() == null) continue;
            BigDecimal diff = item.getCountQty().subtract(item.getSystemQty());
            item.setDiffQty(diff);
            itemMapper.updateById(item);

            if (diff.compareTo(BigDecimal.ZERO) != 0) {
                stockService.adjustStock(
                        item.getInputItemId(), st.getWarehouseId(), diff,
                        "stocktake_adjust", "warehouse_stocktake", st.getId(),
                        operatorId, "Stocktake " + st.getCode() + " diff=" + diff.toPlainString());
            }
        }
        st.setStatus("confirmed");
        st.setConfirmedBy(operatorId);
        st.setConfirmedAt(LocalDateTime.now());
        stocktakeMapper.updateById(st);
        log.info("[Stocktake confirmed] code={}", st.getCode());
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        WarehouseStocktake st = stocktakeMapper.selectById(id);
        if (st == null) throw new BusinessException(R.NOT_FOUND, "Stocktake not found");
        if ("confirmed".equals(st.getStatus())) throw new BusinessException("Cannot cancel confirmed stocktake");
        st.setStatus("cancelled");
        stocktakeMapper.updateById(st);
    }
}
