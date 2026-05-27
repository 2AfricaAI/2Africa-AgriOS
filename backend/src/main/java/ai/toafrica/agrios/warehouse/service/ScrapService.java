package ai.toafrica.agrios.warehouse.service;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.master.service.InputStockService;
import ai.toafrica.agrios.warehouse.entity.WarehouseScrap;
import ai.toafrica.agrios.warehouse.entity.WarehouseScrapItem;
import ai.toafrica.agrios.warehouse.mapper.WarehouseScrapItemMapper;
import ai.toafrica.agrios.warehouse.mapper.WarehouseScrapMapper;
import ai.toafrica.agrios.warehouse.vo.ScrapDetailVO;
import ai.toafrica.agrios.warehouse.vo.ScrapVO;
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
public class ScrapService {
    private final WarehouseScrapMapper scrapMapper;
    private final WarehouseScrapItemMapper itemMapper;
    private final InputStockService stockService;
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    public PageResult<ScrapVO> page(String status, Long warehouseId, String scrapType, PageQuery pq) {
        QueryWrapper<ScrapVO> q = new QueryWrapper<>();
        if (status != null && !status.isBlank()) q.eq("s.status", status.trim());
        if (warehouseId != null) q.eq("s.warehouse_id", warehouseId);
        if (scrapType != null && !scrapType.isBlank()) q.eq("s.scrap_type", scrapType.trim());
        q.orderByDesc("s.created_at");
        return PageResult.of(scrapMapper.pageWithJoin(new Page<>(pq.getPage(), pq.getSize()), q));
    }

    public ScrapDetailVO detail(Long id) {
        QueryWrapper<ScrapVO> q = new QueryWrapper<>();
        q.eq("s.id", id);
        var records = scrapMapper.pageWithJoin(new Page<>(1, 1), q).getRecords();
        if (records.isEmpty()) throw new BusinessException(R.NOT_FOUND, "Scrap order not found");
        ScrapDetailVO vo = new ScrapDetailVO();
        vo.setHeader(records.get(0));
        vo.setItems(itemMapper.findByScrapId(id));
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(Long warehouseId, String scrapType, List<ScrapItemInput> items, String remark) {
        String today = LocalDateTime.now().format(YMD);
        int seq = scrapMapper.countByDate(today) + 1;
        String code = String.format("SC-%s-%04d", today, seq);
        WarehouseScrap sc = new WarehouseScrap();
        sc.setCode(code); sc.setWarehouseId(warehouseId);
        sc.setScrapType(scrapType != null ? scrapType : "damaged");
        sc.setStatus("draft"); sc.setRemark(remark);
        scrapMapper.insert(sc);
        for (ScrapItemInput si : items) {
            WarehouseScrapItem item = new WarehouseScrapItem();
            item.setScrapId(sc.getId()); item.setInputItemId(si.inputItemId);
            item.setQty(si.qty); item.setReason(si.reason);
            itemMapper.insert(item);
        }
        log.info("[Scrap created] code={} warehouse={} type={} items={}", code, warehouseId, scrapType, items.size());
        return sc.getId();
    }

    public record ScrapItemInput(Long inputItemId, BigDecimal qty, String reason) {}

    /** 确认报废 → adjustStock(-qty) + log(damage) */
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long id, Long operatorId) {
        WarehouseScrap sc = scrapMapper.selectById(id);
        if (sc == null) throw new BusinessException(R.NOT_FOUND, "Scrap order not found");
        if (!"draft".equals(sc.getStatus())) throw new BusinessException("Only draft can be confirmed");
        List<WarehouseScrapItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<WarehouseScrapItem>().eq(WarehouseScrapItem::getScrapId, id));
        for (WarehouseScrapItem item : items) {
            stockService.adjustStock(item.getInputItemId(), sc.getWarehouseId(),
                    item.getQty().negate(), "damage", "warehouse_scrap", sc.getId(),
                    operatorId, "Scrap " + sc.getCode() + ": " + (item.getReason() != null ? item.getReason() : sc.getScrapType()));
        }
        sc.setStatus("confirmed"); sc.setConfirmedBy(operatorId); sc.setConfirmedAt(LocalDateTime.now());
        scrapMapper.updateById(sc);
        log.info("[Scrap confirmed] code={}", sc.getCode());
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        WarehouseScrap sc = scrapMapper.selectById(id);
        if (sc == null) throw new BusinessException(R.NOT_FOUND, "Scrap not found");
        if ("confirmed".equals(sc.getStatus())) throw new BusinessException("Cannot cancel confirmed scrap");
        sc.setStatus("cancelled"); scrapMapper.updateById(sc);
    }
}
