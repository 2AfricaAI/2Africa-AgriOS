package ai.toafrica.agrios.sales.service;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.framework.security.SecurityUtil;
import ai.toafrica.agrios.packhouse.entity.Inventory;
import ai.toafrica.agrios.packhouse.entity.InventoryAdjustLog;
import ai.toafrica.agrios.packhouse.mapper.InventoryAdjustLogMapper;
import ai.toafrica.agrios.packhouse.mapper.InventoryMapper;
import ai.toafrica.agrios.sales.entity.Fulfillment;
import ai.toafrica.agrios.sales.entity.FulfillmentItem;
import ai.toafrica.agrios.sales.entity.OrderInventoryLock;
import ai.toafrica.agrios.sales.entity.OrderItem;
import ai.toafrica.agrios.sales.entity.SalesOrder;
import ai.toafrica.agrios.sales.mapper.FulfillmentItemMapper;
import ai.toafrica.agrios.sales.mapper.FulfillmentMapper;
import ai.toafrica.agrios.sales.mapper.OrderInventoryLockMapper;
import ai.toafrica.agrios.sales.mapper.OrderItemMapper;
import ai.toafrica.agrios.sales.mapper.SalesOrderMapper;
import ai.toafrica.agrios.sales.vo.FulfillmentDetailVO;
import ai.toafrica.agrios.sales.vo.FulfillmentItemVO;
import ai.toafrica.agrios.sales.vo.FulfillmentVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 拣货 - 销售链 Sprint 9.3, FEFO 升级 Sprint 26
 *
 * Picking 与 Outbound 共享 fulfillment 表(状态机):
 *   pick(orderId)  → 创建 fulfillment(status=ready), FEFO 锁 inventory, order.status→locked
 *   后续 Sprint 9.4 的 ship() 会把 status 推到 shipped, 并真正扣 qty_locked
 *
 * FEFO 规则 (Sprint 26 替换 FIFO):
 *   - 按 inventory.expiry_date ASC, prod_date ASC, id ASC 排序 (最早到期先出)
 *   - expiry_date 为空 (遗留行) 排在末尾, 兼容 FIFO 回退
 *   - 同一行库存按需精确切片 (qty_avail 不够则跨多行库存合并)
 *   - 任意一个订单行库存不够 → 整个事务回滚
 *
 * 库存原子改动 (每次切一片库存都做):
 *   inventory.qty_avail  -= take
 *   inventory.qty_locked += take
 *   写 adjust_log(adjust_type=lock, reason_code=picking, field_name=qty_avail, qty_change=-take)
 *   写 order_inventory_lock(status=locked, qty_locked=take)
 *   写 fulfillment_item(inventory_id, batch_id, sku_id, qty=take)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PickingService {

    private final FulfillmentMapper fulfillmentMapper;
    private final FulfillmentItemMapper fulfillmentItemMapper;
    private final OrderInventoryLockMapper lockMapper;
    private final SalesOrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final InventoryMapper inventoryMapper;
    private final InventoryAdjustLogMapper adjustLogMapper;

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    // ============================================================
    // 列表
    // ============================================================
    public PageResult<FulfillmentVO> page(Long orderId, Long customerId, String status,
                                          String code, PageQuery pq) {
        QueryWrapper<FulfillmentVO> q = new QueryWrapper<>();
        if (orderId != null) q.eq("f.order_id", orderId);
        if (customerId != null) q.eq("o.customer_id", customerId);
        if (status != null && !status.isBlank()) q.eq("f.status", status.trim());
        if (code != null && !code.isBlank()) q.like("f.code", code.trim());
        q.orderByDesc("f.created_at").orderByDesc("f.id");
        Page<FulfillmentVO> p = new Page<>(pq.getPage(), pq.getSize());
        return PageResult.of(fulfillmentMapper.pageWithJoin(p, q));
    }

    // ============================================================
    // 详情
    // ============================================================
    public FulfillmentDetailVO detail(Long id) {
        QueryWrapper<FulfillmentVO> q = new QueryWrapper<>();
        q.eq("f.id", id);
        Page<FulfillmentVO> p = new Page<>(1, 1);
        var pageData = fulfillmentMapper.pageWithJoin(p, q);
        if (pageData.getRecords().isEmpty()) {
            throw new BusinessException(R.NOT_FOUND, "Fulfillment not found");
        }
        List<FulfillmentItemVO> items = fulfillmentItemMapper.findByFulfillmentId(id);
        FulfillmentDetailVO vo = new FulfillmentDetailVO();
        vo.setFulfillment(pageData.getRecords().get(0));
        vo.setItems(items);
        return vo;
    }

    // ============================================================
    // 列出某订单的全部 fulfillment (在 OrderDetail 页用)
    // ============================================================
    public List<FulfillmentVO> listByOrder(Long orderId) {
        QueryWrapper<FulfillmentVO> q = new QueryWrapper<>();
        q.eq("f.order_id", orderId);
        q.orderByDesc("f.id");
        Page<FulfillmentVO> p = new Page<>(1, 100);
        return fulfillmentMapper.pageWithJoin(p, q).getRecords();
    }

    // ============================================================
    // 拣货核心 — FIFO 锁库存, 一次性把订单所有 item 拣完
    // ============================================================
    @Transactional(rollbackFor = Exception.class)
    public Long pick(Long orderId) {
        SalesOrder order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException(R.NOT_FOUND, "Order not found");
        if (!"confirmed".equals(order.getStatus())) {
            throw new BusinessException(
                "Only confirmed orders can be picked (current: " + order.getStatus() + ")");
        }

        // 防重: 一个订单只允许一个非 cancelled 的 fulfillment
        Long existing = countActiveFulfillment(orderId);
        if (existing > 0) {
            throw new BusinessException("Order already has an active fulfillment");
        }

        // 1. 读订单全部明细
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        if (items.isEmpty()) throw new BusinessException("Order has no line items");

        // 2. 创建 fulfillment 头 (先 picking, 拣完成功再改 ready)
        Long pickerId = SecurityUtil.currentUserId();
        LocalDate today = LocalDate.now();
        int seq = fulfillmentMapper.countByDate(today) + 1;
        String code = String.format("SH-%s-%03d", today.format(YMD), seq);

        Fulfillment f = new Fulfillment();
        f.setCode(code);
        f.setOrderId(orderId);
        f.setPickerId(pickerId);
        f.setStatus("picking");
        fulfillmentMapper.insert(f);

        // 3. 逐行 FIFO 拣货
        for (OrderItem item : items) {
            BigDecimal needed = item.getQty();
            // Sprint 26: FEFO replaces FIFO — pick earliest-expiry first.
            List<Inventory> available = inventoryMapper.findAvailableBySkuFefo(item.getSkuId());

            for (Inventory inv : available) {
                if (needed.signum() <= 0) break;

                BigDecimal take = needed.min(inv.getQtyAvail());

                // a) 改 inventory 数量
                BigDecimal availBefore = inv.getQtyAvail();
                BigDecimal availAfter  = availBefore.subtract(take);
                inv.setQtyAvail(availAfter);
                inv.setQtyLocked(inv.getQtyLocked().add(take));
                int rows = inventoryMapper.updateById(inv);
                if (rows == 0) {
                    // 乐观锁冲突 → 整体回滚, 由调用方重试
                    throw new BusinessException(
                        "Inventory was modified by another transaction (SKU " + item.getSkuId() + "), please retry");
                }

                // b) 写 adjust_log (qty_avail 减)
                InventoryAdjustLog logEntry = new InventoryAdjustLog();
                logEntry.setInventoryId(inv.getId());
                logEntry.setAdjustType("lock");
                logEntry.setReasonCode("picking");
                logEntry.setQtyBefore(availBefore);
                logEntry.setQtyChange(take.negate());
                logEntry.setQtyAfter(availAfter);
                logEntry.setFieldName("qty_avail");
                logEntry.setRefType("fulfillment");
                logEntry.setRefId(f.getId());
                logEntry.setRemark("Picked for order " + order.getCode());
                logEntry.setOperatorId(pickerId);
                adjustLogMapper.insert(logEntry);

                // c) 写 order_inventory_lock
                OrderInventoryLock lock = new OrderInventoryLock();
                lock.setOrderId(orderId);
                lock.setOrderItemId(item.getId());
                lock.setInventoryId(inv.getId());
                lock.setQtyLocked(take);
                lock.setStatus("locked");
                lockMapper.insert(lock);

                // d) 写 fulfillment_item
                FulfillmentItem fi = new FulfillmentItem();
                fi.setFulfillmentId(f.getId());
                fi.setOrderItemId(item.getId());
                fi.setInventoryId(inv.getId());
                fi.setBatchId(inv.getBatchId());
                fi.setSkuId(inv.getSkuId());
                fi.setQty(take);
                fulfillmentItemMapper.insert(fi);

                needed = needed.subtract(take);
            }

            if (needed.signum() > 0) {
                throw new BusinessException(String.format(
                    "Not enough available inventory for SKU id=%d, short by %s units",
                    item.getSkuId(), needed.toPlainString()));
            }
        }

        // 4. fulfillment → ready, order → locked
        f.setStatus("ready");
        fulfillmentMapper.updateById(f);

        order.setStatus("locked");
        orderMapper.updateById(order);

        log.info("[Picking complete] order={} fulfillment={} items={}",
                order.getCode(), f.getCode(), items.size());
        return f.getId();
    }

    // ============================================================
    // 取消拣货 — 释放锁定, fulfillment → cancelled, order → confirmed
    // ============================================================
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long fulfillmentId) {
        Fulfillment f = fulfillmentMapper.selectById(fulfillmentId);
        if (f == null) throw new BusinessException(R.NOT_FOUND, "Fulfillment not found");
        if (!"ready".equals(f.getStatus()) && !"picking".equals(f.getStatus())) {
            throw new BusinessException(
                "Only ready/picking fulfillments can be cancelled (current: " + f.getStatus() + ")");
        }

        Long operatorId = SecurityUtil.currentUserId();
        SalesOrder order = orderMapper.selectById(f.getOrderId());

        // 释放 inventory: 反向把 qty_locked 还回 qty_avail
        List<OrderInventoryLock> locks = lockMapper.selectList(
                new LambdaQueryWrapper<OrderInventoryLock>()
                        .eq(OrderInventoryLock::getOrderId, f.getOrderId())
                        .eq(OrderInventoryLock::getStatus, "locked"));

        for (OrderInventoryLock lock : locks) {
            Inventory inv = inventoryMapper.selectById(lock.getInventoryId());
            if (inv == null) continue;
            BigDecimal release = lock.getQtyLocked();
            BigDecimal lockedBefore = inv.getQtyLocked();
            BigDecimal availBefore = inv.getQtyAvail();
            inv.setQtyLocked(lockedBefore.subtract(release));
            inv.setQtyAvail(availBefore.add(release));
            inventoryMapper.updateById(inv);

            InventoryAdjustLog logEntry = new InventoryAdjustLog();
            logEntry.setInventoryId(inv.getId());
            logEntry.setAdjustType("unlock");
            logEntry.setReasonCode("picking_cancel");
            logEntry.setQtyBefore(availBefore);
            logEntry.setQtyChange(release);
            logEntry.setQtyAfter(availBefore.add(release));
            logEntry.setFieldName("qty_avail");
            logEntry.setRefType("fulfillment");
            logEntry.setRefId(f.getId());
            logEntry.setRemark("Picking cancelled for fulfillment " + f.getCode());
            logEntry.setOperatorId(operatorId);
            adjustLogMapper.insert(logEntry);

            lock.setStatus("released");
            lockMapper.updateById(lock);
        }

        f.setStatus("cancelled");
        fulfillmentMapper.updateById(f);

        // Order returns to confirmed, can be re-picked
        if (order != null && "locked".equals(order.getStatus())) {
            order.setStatus("confirmed");
            orderMapper.updateById(order);
        }

        log.info("[Picking cancelled] fulfillment={}", f.getCode());
    }

    /** Count non-cancelled fulfillments on an order (used for dedup before picking). */
    private Long countActiveFulfillment(Long orderId) {
        return fulfillmentMapper.selectCount(
                new LambdaQueryWrapper<Fulfillment>()
                        .eq(Fulfillment::getOrderId, orderId)
                        .ne(Fulfillment::getStatus, "cancelled"));
    }
}
