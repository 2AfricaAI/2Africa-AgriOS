package ai.toafrica.agrios.sales.service;

import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.framework.security.SecurityUtil;
import ai.toafrica.agrios.packhouse.entity.Inventory;
import ai.toafrica.agrios.packhouse.entity.InventoryAdjustLog;
import ai.toafrica.agrios.packhouse.mapper.InventoryAdjustLogMapper;
import ai.toafrica.agrios.packhouse.mapper.InventoryMapper;
import ai.toafrica.agrios.sales.dto.ShipForm;
import ai.toafrica.agrios.sales.entity.Fulfillment;
import ai.toafrica.agrios.sales.entity.FulfillmentItem;
import ai.toafrica.agrios.sales.entity.OrderInventoryLock;
import ai.toafrica.agrios.sales.entity.OrderItem;
import ai.toafrica.agrios.sales.entity.Revenue;
import ai.toafrica.agrios.sales.entity.SalesOrder;
import ai.toafrica.agrios.sales.mapper.FulfillmentItemMapper;
import ai.toafrica.agrios.sales.mapper.FulfillmentMapper;
import ai.toafrica.agrios.sales.mapper.OrderInventoryLockMapper;
import ai.toafrica.agrios.sales.mapper.OrderItemMapper;
import ai.toafrica.agrios.sales.mapper.RevenueMapper;
import ai.toafrica.agrios.sales.mapper.SalesOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 出库 - 销售链 Sprint 9.4
 *
 * ship(fulfillmentId, form): ready → shipped
 *   1. 真正扣 inventory.qty_locked (锁定库存彻底消失)
 *   2. order_inventory_lock.status → shipped
 *   3. 推 order_item.qty_shipped (累加本次发货量)
 *   4. fulfillment.status → shipped, 写承运商/司机/车牌信息
 *   5. sales_order.status → shipping (部分发货) or completed (全部发完)
 *   6. **按 OrderItem 粒度生成 Revenue 行** (V2.0 Phase 2 P&L 事实表的种子)
 *
 * deliver(fulfillmentId): shipped → delivered (可选, 用于客户签收记录)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboundService {

    private final FulfillmentMapper fulfillmentMapper;
    private final FulfillmentItemMapper fulfillmentItemMapper;
    private final OrderInventoryLockMapper lockMapper;
    private final SalesOrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final InventoryMapper inventoryMapper;
    private final InventoryAdjustLogMapper adjustLogMapper;
    private final RevenueMapper revenueMapper;

    @Transactional(rollbackFor = Exception.class)
    public void ship(Long fulfillmentId, ShipForm form) {
        Fulfillment f = fulfillmentMapper.selectById(fulfillmentId);
        if (f == null) throw new BusinessException(R.NOT_FOUND, "Fulfillment not found");
        if (!"ready".equals(f.getStatus())) {
            throw new BusinessException(
                "Only 'ready' fulfillments can be shipped (current: " + f.getStatus() + ")");
        }

        Long operatorId = SecurityUtil.currentUserId();
        LocalDateTime now = LocalDateTime.now();
        SalesOrder order = orderMapper.selectById(f.getOrderId());
        if (order == null) throw new BusinessException("Order missing for fulfillment");

        // 1. 收集本次发货明细
        List<FulfillmentItem> items = fulfillmentItemMapper.selectList(
                new LambdaQueryWrapper<FulfillmentItem>().eq(FulfillmentItem::getFulfillmentId, fulfillmentId));
        if (items.isEmpty()) throw new BusinessException("Fulfillment has no items");

        // 2. 按 inventory_id 合并(同一库存行可能跨多个订单 item), 扣 qty_locked + 写 adjust_log
        Map<Long, BigDecimal> qtyByInv = new HashMap<>();
        for (FulfillmentItem fi : items) {
            qtyByInv.merge(fi.getInventoryId(), fi.getQty(), BigDecimal::add);
        }
        for (Map.Entry<Long, BigDecimal> e : qtyByInv.entrySet()) {
            Inventory inv = inventoryMapper.selectById(e.getKey());
            if (inv == null) {
                throw new BusinessException("Inventory row " + e.getKey() + " no longer exists");
            }
            BigDecimal lockedBefore = inv.getQtyLocked();
            BigDecimal lockedAfter  = lockedBefore.subtract(e.getValue());
            if (lockedAfter.signum() < 0) {
                throw new BusinessException("Inventory " + inv.getId() +
                    " qty_locked would go negative — concurrent modification?");
            }
            inv.setQtyLocked(lockedAfter);
            inv.setLastOpAt(now);
            int rows = inventoryMapper.updateById(inv);
            if (rows == 0) {
                throw new BusinessException("Optimistic lock conflict on inventory " + inv.getId() + ", please retry");
            }

            InventoryAdjustLog logRow = new InventoryAdjustLog();
            logRow.setInventoryId(inv.getId());
            logRow.setAdjustType("out");
            logRow.setReasonCode("sale");
            logRow.setQtyBefore(lockedBefore);
            logRow.setQtyChange(e.getValue().negate());
            logRow.setQtyAfter(lockedAfter);
            logRow.setFieldName("qty_locked");
            logRow.setRefType("fulfillment");
            logRow.setRefId(f.getId());
            logRow.setRemark("Shipped via fulfillment " + f.getCode() + " (order " + order.getCode() + ")");
            logRow.setOperatorId(operatorId);
            adjustLogMapper.insert(logRow);
        }

        // 3. order_inventory_lock.status = shipped (对该 fulfillment 涉及的所有 lock 行)
        lockMapper.update(null, new LambdaUpdateWrapper<OrderInventoryLock>()
                .eq(OrderInventoryLock::getOrderId, f.getOrderId())
                .eq(OrderInventoryLock::getStatus, "locked")
                .set(OrderInventoryLock::getStatus, "shipped"));

        // 4. 推 order_item.qty_shipped 并判断是否完全发完
        Map<Long, BigDecimal> shippedByItem = new HashMap<>();
        for (FulfillmentItem fi : items) {
            shippedByItem.merge(fi.getOrderItemId(), fi.getQty(), BigDecimal::add);
        }

        List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, f.getOrderId()));

        boolean allShipped = true;
        Map<Long, OrderItem> orderItemMap = new HashMap<>();
        for (OrderItem oi : orderItems) {
            BigDecimal add = shippedByItem.getOrDefault(oi.getId(), BigDecimal.ZERO);
            BigDecimal newShipped = oi.getQtyShipped().add(add);
            oi.setQtyShipped(newShipped);
            orderItemMapper.updateById(oi);
            orderItemMap.put(oi.getId(), oi);
            if (newShipped.compareTo(oi.getQty()) < 0) allShipped = false;
        }

        // 5. 更新 fulfillment 头
        f.setStatus("shipped");
        f.setShipAt(now);
        if (form != null) {
            if (form.getShipMethod() != null && !form.getShipMethod().isBlank()) f.setShipMethod(form.getShipMethod());
            if (form.getTrackNo() != null) f.setTrackNo(form.getTrackNo());
            if (form.getDriverName() != null) f.setDriverName(form.getDriverName());
            if (form.getDriverPhone() != null) f.setDriverPhone(form.getDriverPhone());
            if (form.getVehicleNo() != null) f.setVehicleNo(form.getVehicleNo());
            if (form.getRemark() != null && !form.getRemark().isBlank()) f.setRemark(form.getRemark());
        }
        fulfillmentMapper.updateById(f);

        // 6. 推订单状态: 全发→completed, 部分→shipping
        order.setStatus(allShipped ? "completed" : "shipping");
        orderMapper.updateById(order);

        // 7. 生成 Revenue 流水 (按本次实际发货的 OrderItem)
        LocalDate today = LocalDate.now();
        int revRows = 0;
        for (Map.Entry<Long, BigDecimal> e : shippedByItem.entrySet()) {
            OrderItem oi = orderItemMap.get(e.getKey());
            if (oi == null) continue;
            BigDecimal qty = e.getValue();
            if (qty.signum() <= 0) continue;

            BigDecimal gross = qty.multiply(oi.getUnitPrice());
            Revenue r = new Revenue();
            r.setOrderId(order.getId());
            r.setOrderItemId(oi.getId());
            r.setFulfillmentId(f.getId());
            r.setSkuId(oi.getSkuId());
            r.setCustomerId(order.getCustomerId());
            // 主要批次 = 该 OrderItem 对应 fulfillment 里第一个 batch
            Long batchId = items.stream()
                    .filter(fi -> fi.getOrderItemId().equals(oi.getId()))
                    .map(FulfillmentItem::getBatchId)
                    .findFirst().orElse(null);
            r.setBatchId(batchId);
            r.setQty(qty);
            r.setGrossAmount(gross);
            r.setTax(BigDecimal.ZERO);   // VAT 16% 留到 Phase 2 多币种 sprint 处理
            r.setNetAmount(gross);
            r.setCurrency(order.getCurrency());
            r.setRecognitionDate(today);
            r.setStatus("recognized");
            r.setRemark("Auto-generated by fulfillment " + f.getCode());
            revenueMapper.insert(r);
            revRows++;
        }

        log.info("[Shipped] fulfillment={} order={}→{} inventoryRowsTouched={} revenueRows={} allShipped={}",
                f.getCode(), order.getCode(), order.getStatus(),
                qtyByInv.size(), revRows, allShipped);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deliver(Long fulfillmentId) {
        Fulfillment f = fulfillmentMapper.selectById(fulfillmentId);
        if (f == null) throw new BusinessException(R.NOT_FOUND, "Fulfillment not found");
        if (!"shipped".equals(f.getStatus())) {
            throw new BusinessException(
                "Only 'shipped' fulfillments can be marked delivered (current: " + f.getStatus() + ")");
        }
        f.setStatus("delivered");
        f.setDeliveredAt(LocalDateTime.now());
        fulfillmentMapper.updateById(f);
        log.info("[Delivered] fulfillment={}", f.getCode());
    }
}
