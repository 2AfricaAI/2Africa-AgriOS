package ai.toafrica.agrios.warehouse.service;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.master.service.InputStockService;
import ai.toafrica.agrios.warehouse.entity.WarehouseInbound;
import ai.toafrica.agrios.warehouse.entity.WarehouseInboundItem;
import ai.toafrica.agrios.warehouse.mapper.WarehouseInboundItemMapper;
import ai.toafrica.agrios.warehouse.mapper.WarehouseInboundMapper;
import ai.toafrica.agrios.warehouse.vo.InboundDetailVO;
import ai.toafrica.agrios.warehouse.vo.InboundItemVO;
import ai.toafrica.agrios.warehouse.vo.InboundVO;
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
 * 入库单服务 (Sprint 22.4)
 *
 * 流程: PO receive → createFromPO (draft) → confirm (库存 +qty)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InboundService {

    private final WarehouseInboundMapper inboundMapper;
    private final WarehouseInboundItemMapper itemMapper;
    private final InputStockService stockService;

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    // ============================================================
    // 列表
    // ============================================================
    public PageResult<InboundVO> page(String status, Long warehouseId, String sourceType, PageQuery pq) {
        QueryWrapper<InboundVO> q = new QueryWrapper<>();
        if (status != null && !status.isBlank()) q.eq("i.status", status.trim());
        if (warehouseId != null) q.eq("i.warehouse_id", warehouseId);
        if (sourceType != null && !sourceType.isBlank()) q.eq("i.source_type", sourceType.trim());
        q.orderByDesc("i.created_at");

        Page<InboundVO> p = new Page<>(pq.getPage(), pq.getSize());
        return PageResult.of(inboundMapper.pageWithJoin(p, q));
    }

    // ============================================================
    // 详情
    // ============================================================
    public InboundDetailVO detail(Long id) {
        WarehouseInbound h = inboundMapper.selectById(id);
        if (h == null) throw new BusinessException(R.NOT_FOUND, "Inbound order not found");
        // header via JOIN
        QueryWrapper<InboundVO> q = new QueryWrapper<>();
        q.eq("i.id", id);
        Page<InboundVO> p = new Page<>(1, 1);
        var records = inboundMapper.pageWithJoin(p, q).getRecords();
        if (records.isEmpty()) throw new BusinessException(R.NOT_FOUND, "Inbound order not found");
        List<InboundItemVO> items = itemMapper.findByInboundId(id);
        InboundDetailVO vo = new InboundDetailVO();
        vo.setHeader(records.get(0));
        vo.setItems(items);
        return vo;
    }

    // ============================================================
    // PO 收货 → 生成入库单草稿 (由 PurchaseOrderService 调用)
    // ============================================================
    @Transactional(rollbackFor = Exception.class)
    public Long createFromPO(Long poId, Long warehouseId, List<PoItemInput> poItems) {
        String today = LocalDateTime.now().format(YMD);
        int seq = inboundMapper.countByDate(today) + 1;
        String code = String.format("IN-%s-%04d", today, seq);

        WarehouseInbound inbound = new WarehouseInbound();
        inbound.setCode(code);
        inbound.setSourceType("po_receive");
        inbound.setSourceId(poId);
        inbound.setWarehouseId(warehouseId);
        inbound.setStatus("draft");
        inboundMapper.insert(inbound);

        for (PoItemInput pi : poItems) {
            WarehouseInboundItem item = new WarehouseInboundItem();
            item.setInboundId(inbound.getId());
            item.setInputItemId(pi.inputItemId);
            item.setExpectedQty(pi.expectedQty);
            item.setActualQty(null);  // 待仓库确认时填
            itemMapper.insert(item);
        }

        log.info("[Inbound created] code={} po={} warehouse={} items={}",
                code, poId, warehouseId, poItems.size());
        return inbound.getId();
    }

    /** PO 明细行简单结构 */
    public record PoItemInput(Long inputItemId, BigDecimal expectedQty) {}

    // ============================================================
    // 手工新建入库单 (Sprint 22.9a)
    // ============================================================
    @Transactional(rollbackFor = Exception.class)
    public Long createManual(Long warehouseId, String sourceType, List<PoItemInput> items, String remark) {
        String today = LocalDateTime.now().format(YMD);
        int seq = inboundMapper.countByDate(today) + 1;
        String code = String.format("IN-%s-%04d", today, seq);

        WarehouseInbound inbound = new WarehouseInbound();
        inbound.setCode(code);
        inbound.setSourceType(sourceType != null ? sourceType : "manual");
        inbound.setSourceId(null);
        inbound.setWarehouseId(warehouseId);
        inbound.setStatus("draft");
        inbound.setRemark(remark);
        inboundMapper.insert(inbound);

        for (PoItemInput pi : items) {
            WarehouseInboundItem item = new WarehouseInboundItem();
            item.setInboundId(inbound.getId());
            item.setInputItemId(pi.inputItemId);
            item.setExpectedQty(pi.expectedQty);
            item.setActualQty(null);
            itemMapper.insert(item);
        }
        log.info("[Inbound manual] code={} warehouse={} type={} items={}",
                code, warehouseId, sourceType, items.size());
        return inbound.getId();
    }

    // ============================================================
    // 确认入库 (仓库人员操作)
    // ============================================================
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long id, List<ConfirmItem> confirmItems, Long operatorId) {
        WarehouseInbound inbound = inboundMapper.selectById(id);
        if (inbound == null) throw new BusinessException(R.NOT_FOUND, "Inbound order not found");
        if (!"draft".equals(inbound.getStatus())) {
            throw new BusinessException("Only draft inbound orders can be confirmed (current: " + inbound.getStatus() + ")");
        }

        // 更新每行的 actual_qty + 调库存
        for (ConfirmItem ci : confirmItems) {
            WarehouseInboundItem item = itemMapper.selectById(ci.itemId);
            if (item == null || !item.getInboundId().equals(id)) continue;
            item.setActualQty(ci.actualQty);
            item.setRemark(ci.remark);
            itemMapper.updateById(item);

            // 实际入库量入库
            if (ci.actualQty != null && ci.actualQty.compareTo(BigDecimal.ZERO) > 0) {
                stockService.adjustStock(
                        item.getInputItemId(),
                        inbound.getWarehouseId(),
                        ci.actualQty,
                        "po_receive",
                        "warehouse_inbound",
                        inbound.getId(),
                        operatorId,
                        "Inbound " + inbound.getCode()
                );
            }
        }

        inbound.setStatus("confirmed");
        inbound.setConfirmedBy(operatorId);
        inbound.setConfirmedAt(LocalDateTime.now());
        inboundMapper.updateById(inbound);
        log.info("[Inbound confirmed] code={} by={}", inbound.getCode(), operatorId);
    }

    public record ConfirmItem(Long itemId, BigDecimal actualQty, String remark) {}

    // ============================================================
    // 取消
    // ============================================================
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        WarehouseInbound inbound = inboundMapper.selectById(id);
        if (inbound == null) throw new BusinessException(R.NOT_FOUND, "Inbound order not found");
        if (!"draft".equals(inbound.getStatus())) {
            throw new BusinessException("Only draft inbound orders can be cancelled");
        }
        inbound.setStatus("cancelled");
        inboundMapper.updateById(inbound);
        log.info("[Inbound cancelled] code={}", inbound.getCode());
    }
}
