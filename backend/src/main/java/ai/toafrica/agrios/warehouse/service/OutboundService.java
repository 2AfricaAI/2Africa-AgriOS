package ai.toafrica.agrios.warehouse.service;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.master.service.InputStockService;
import ai.toafrica.agrios.warehouse.entity.WarehouseOutbound;
import ai.toafrica.agrios.warehouse.entity.WarehouseOutboundItem;
import ai.toafrica.agrios.warehouse.mapper.WarehouseOutboundItemMapper;
import ai.toafrica.agrios.warehouse.mapper.WarehouseOutboundMapper;
import ai.toafrica.agrios.warehouse.vo.OutboundDetailVO;
import ai.toafrica.agrios.warehouse.vo.OutboundItemVO;
import ai.toafrica.agrios.warehouse.vo.OutboundVO;
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

/**
 * 出库单服务 (Sprint 22.5)
 *
 * 流程: draft → picked → confirmed (扣库存)
 *       draft → cancelled
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboundService {

    private final WarehouseOutboundMapper outboundMapper;
    private final WarehouseOutboundItemMapper itemMapper;
    private final InputStockService stockService;

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    // ============================================================
    // 列表
    // ============================================================
    public PageResult<OutboundVO> page(String status, Long warehouseId, String sourceType, PageQuery pq) {
        QueryWrapper<OutboundVO> q = new QueryWrapper<>();
        if (status != null && !status.isBlank()) q.eq("o.status", status.trim());
        if (warehouseId != null) q.eq("o.warehouse_id", warehouseId);
        if (sourceType != null && !sourceType.isBlank()) q.eq("o.source_type", sourceType.trim());
        q.orderByDesc("o.created_at");
        Page<OutboundVO> p = new Page<>(pq.getPage(), pq.getSize());
        return PageResult.of(outboundMapper.pageWithJoin(p, q));
    }

    // ============================================================
    // 详情
    // ============================================================
    public OutboundDetailVO detail(Long id) {
        QueryWrapper<OutboundVO> q = new QueryWrapper<>();
        q.eq("o.id", id);
        Page<OutboundVO> p = new Page<>(1, 1);
        var records = outboundMapper.pageWithJoin(p, q).getRecords();
        if (records.isEmpty()) throw new BusinessException(R.NOT_FOUND, "Outbound order not found");
        List<OutboundItemVO> items = itemMapper.findByOutboundId(id);
        OutboundDetailVO vo = new OutboundDetailVO();
        vo.setHeader(records.get(0));
        vo.setItems(items);
        return vo;
    }

    // ============================================================
    // 手工创建出库单 (或由 Activity 自动触发)
    // ============================================================
    @Transactional(rollbackFor = Exception.class)
    public Long create(String sourceType, Long sourceId, Long warehouseId,
                       List<OutboundItemInput> requestedItems) {
        String today = LocalDateTime.now().format(YMD);
        int seq = outboundMapper.countByDate(today) + 1;
        String code = String.format("OUT-%s-%04d", today, seq);

        WarehouseOutbound ob = new WarehouseOutbound();
        ob.setCode(code);
        ob.setSourceType(sourceType);
        ob.setSourceId(sourceId);
        ob.setWarehouseId(warehouseId);
        ob.setStatus("draft");
        outboundMapper.insert(ob);

        for (OutboundItemInput ri : requestedItems) {
            WarehouseOutboundItem item = new WarehouseOutboundItem();
            item.setOutboundId(ob.getId());
            item.setInputItemId(ri.inputItemId);
            item.setRequestedQty(ri.requestedQty);
            itemMapper.insert(item);
        }
        log.info("[Outbound created] code={} source={}:{} warehouse={} items={}",
                code, sourceType, sourceId, warehouseId, requestedItems.size());
        return ob.getId();
    }

    public record OutboundItemInput(Long inputItemId, BigDecimal requestedQty) {}

    // ============================================================
    // 拣货 (picker 填 picked_qty)
    // ============================================================
    @Transactional(rollbackFor = Exception.class)
    public void pick(Long id, List<PickItem> pickItems, Long pickerId) {
        WarehouseOutbound ob = outboundMapper.selectById(id);
        if (ob == null) throw new BusinessException(R.NOT_FOUND, "Outbound order not found");
        if (!"draft".equals(ob.getStatus()))
            throw new BusinessException("Only draft outbound can be picked (current: " + ob.getStatus() + ")");

        for (PickItem pi : pickItems) {
            WarehouseOutboundItem item = itemMapper.selectById(pi.itemId);
            if (item == null || !item.getOutboundId().equals(id)) continue;
            item.setPickedQty(pi.pickedQty);
            item.setRemark(pi.remark);
            itemMapper.updateById(item);
        }
        ob.setStatus("picked");
        ob.setPickedBy(pickerId);
        ob.setPickedAt(LocalDateTime.now());
        outboundMapper.updateById(ob);
        log.info("[Outbound picked] code={} by={}", ob.getCode(), pickerId);
    }

    public record PickItem(Long itemId, BigDecimal pickedQty, String remark) {}

    // ============================================================
    // 确认出库 (扣库存 + 写流水)
    // ============================================================
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long id, Long operatorId) {
        WarehouseOutbound ob = outboundMapper.selectById(id);
        if (ob == null) throw new BusinessException(R.NOT_FOUND, "Outbound order not found");
        if (!"picked".equals(ob.getStatus()))
            throw new BusinessException("Only picked outbound can be confirmed (current: " + ob.getStatus() + ")");

        List<WarehouseOutboundItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<WarehouseOutboundItem>().eq(WarehouseOutboundItem::getOutboundId, id));

        for (WarehouseOutboundItem item : items) {
            BigDecimal actualQty = item.getPickedQty() != null ? item.getPickedQty() : item.getRequestedQty();
            item.setActualQty(actualQty);
            itemMapper.updateById(item);

            if (actualQty.compareTo(BigDecimal.ZERO) > 0) {
                stockService.adjustStock(
                        item.getInputItemId(),
                        ob.getWarehouseId(),
                        actualQty.negate(),                         // 负数 = 出库
                        "activity_consume",                         // reasonType (default, caller can override via sourceType)
                        "warehouse_outbound",                       // referenceType
                        ob.getId(),                                 // referenceId
                        operatorId,
                        "Outbound " + ob.getCode()
                );
            }
        }
        ob.setStatus("confirmed");
        ob.setConfirmedBy(operatorId);
        ob.setConfirmedAt(LocalDateTime.now());
        outboundMapper.updateById(ob);
        log.info("[Outbound confirmed] code={} items={}", ob.getCode(), items.size());
    }

    // ============================================================
    // 取消
    // ============================================================
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        WarehouseOutbound ob = outboundMapper.selectById(id);
        if (ob == null) throw new BusinessException(R.NOT_FOUND, "Outbound order not found");
        if ("confirmed".equals(ob.getStatus()))
            throw new BusinessException("Cannot cancel a confirmed outbound order");
        ob.setStatus("cancelled");
        outboundMapper.updateById(ob);
        log.info("[Outbound cancelled] code={}", ob.getCode());
    }
}
