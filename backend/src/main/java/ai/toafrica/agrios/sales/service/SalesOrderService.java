package ai.toafrica.agrios.sales.service;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.packhouse.entity.Sku;
import ai.toafrica.agrios.packhouse.mapper.SkuMapper;
import ai.toafrica.agrios.sales.dto.SalesOrderForm;
import ai.toafrica.agrios.sales.entity.Customer;
import ai.toafrica.agrios.sales.entity.OrderItem;
import ai.toafrica.agrios.sales.entity.SalesOrder;
import ai.toafrica.agrios.sales.mapper.CustomerMapper;
import ai.toafrica.agrios.sales.mapper.OrderItemMapper;
import ai.toafrica.agrios.sales.mapper.SalesOrderMapper;
import ai.toafrica.agrios.sales.vo.OrderItemVO;
import ai.toafrica.agrios.sales.vo.SalesOrderDetailVO;
import ai.toafrica.agrios.sales.vo.SalesOrderVO;
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
import java.util.List;
import java.util.Set;

/**
 * 销售订单 - 头/明细 + 状态机
 *   pending  (new)
 *     → confirmed (manager 确认)
 *     → locked    (Picking 锁库存后, 由 PickingService 推进)
 *     → shipping  (Outbound 出库时, 由 OutboundService 推进)
 *     → shipped / delivered / completed
 *     → cancelled (草稿/确认状态可取消, 锁定后不可)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalesOrderService {

    private final SalesOrderMapper orderMapper;
    private final OrderItemMapper itemMapper;
    private final CustomerMapper customerMapper;
    private final SkuMapper skuMapper;

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 草稿/确认 状态都允许编辑;其他状态不可改 */
    private static final Set<String> EDITABLE_STATUSES = Set.of("pending", "confirmed");

    // ============================================================
    // 列表
    // ============================================================
    public PageResult<SalesOrderVO> page(Long customerId, String status, String code,
                                          LocalDate dateFrom, LocalDate dateTo,
                                          PageQuery pq) {
        QueryWrapper<SalesOrderVO> q = new QueryWrapper<>();
        q.isNull("o.deleted_at");
        if (customerId != null) q.eq("o.customer_id", customerId);
        if (status != null && !status.isBlank()) q.eq("o.status", status.trim());
        if (code != null && !code.isBlank()) q.like("o.code", code.trim());
        if (dateFrom != null) q.ge("o.order_date", dateFrom);
        if (dateTo != null) q.le("o.order_date", dateTo);
        q.orderByDesc("o.order_date").orderByDesc("o.id");

        Page<SalesOrderVO> p = new Page<>(pq.getPage(), pq.getSize());
        return PageResult.of(orderMapper.pageWithJoin(p, q));
    }

    // ============================================================
    // 详情 (header + items)
    // ============================================================
    public SalesOrderDetailVO detail(Long id) {
        QueryWrapper<SalesOrderVO> q = new QueryWrapper<>();
        q.isNull("o.deleted_at");
        q.eq("o.id", id);
        Page<SalesOrderVO> p = new Page<>(1, 1);
        var pageData = orderMapper.pageWithJoin(p, q);
        if (pageData.getRecords().isEmpty()) {
            throw new BusinessException(R.NOT_FOUND, "Order not found");
        }
        List<OrderItemVO> items = itemMapper.findByOrderId(id);
        SalesOrderDetailVO vo = new SalesOrderDetailVO();
        vo.setOrder(pageData.getRecords().get(0));
        vo.setItems(items);
        return vo;
    }

    // ============================================================
    // 创建 - 草稿
    // ============================================================
    @Transactional(rollbackFor = Exception.class)
    public Long create(SalesOrderForm form) {
        validateCustomer(form.getCustomerId());
        validateAndEnrichItems(form.getItems());

        // 生成单号
        int seq = orderMapper.countByDate(form.getOrderDate()) + 1;
        String code = String.format("SO-%s-%03d", form.getOrderDate().format(YMD), seq);

        SalesOrder o = new SalesOrder();
        o.setCode(code);
        o.setCustomerId(form.getCustomerId());
        o.setOrderDate(form.getOrderDate());
        o.setDeliveryDate(form.getDeliveryDate());
        o.setShipTo(form.getShipTo());
        o.setCurrency(form.getCurrency());
        o.setStatus("pending");
        o.setPaymentStatus("unpaid");
        o.setPaidAmount(BigDecimal.ZERO);
        // 自动算应付日 = 下单日 + 客户.credit_days (0 = 现结, 7 = 周结, 30 = 月结)
        Customer c = customerMapper.selectById(form.getCustomerId());
        int creditDays = (c != null && c.getCreditDays() != null) ? c.getCreditDays() : 0;
        o.setDueDate(form.getOrderDate().plusDays(creditDays));
        o.setRemark(form.getRemark());
        o.setTotalAmount(BigDecimal.ZERO); // 待 items 算完更新
        orderMapper.insert(o);

        BigDecimal total = insertItems(o.getId(), form.getItems());

        o.setTotalAmount(total);
        orderMapper.updateById(o);

        log.info("[Order created] code={} customer={} total={} {}", code, form.getCustomerId(),
                total.toPlainString(), form.getCurrency());
        return o.getId();
    }

    // ============================================================
    // 编辑 (只允许 pending / confirmed)
    //   header + items 全替换
    // ============================================================
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SalesOrderForm form) {
        SalesOrder o = orderMapper.selectById(id);
        if (o == null) throw new BusinessException(R.NOT_FOUND, "Order not found");
        if (!EDITABLE_STATUSES.contains(o.getStatus())) {
            throw new BusinessException("Cannot edit order in status: " + o.getStatus());
        }
        validateCustomer(form.getCustomerId());
        validateAndEnrichItems(form.getItems());

        o.setCustomerId(form.getCustomerId());
        o.setOrderDate(form.getOrderDate());
        o.setDeliveryDate(form.getDeliveryDate());
        o.setShipTo(form.getShipTo());
        o.setCurrency(form.getCurrency());
        o.setRemark(form.getRemark());

        // 删旧 items, 写新 items
        itemMapper.delete(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id));
        BigDecimal total = insertItems(id, form.getItems());
        o.setTotalAmount(total);
        orderMapper.updateById(o);
    }

    // ============================================================
    // 状态机
    // ============================================================
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long id) {
        SalesOrder o = orderMapper.selectById(id);
        if (o == null) throw new BusinessException(R.NOT_FOUND, "Order not found");
        if (!"pending".equals(o.getStatus())) {
            throw new BusinessException("Only pending orders can be confirmed (current: " + o.getStatus() + ")");
        }
        o.setStatus("confirmed");
        orderMapper.updateById(o);
        log.info("[Order confirmed] code={}", o.getCode());
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        SalesOrder o = orderMapper.selectById(id);
        if (o == null) throw new BusinessException(R.NOT_FOUND, "Order not found");
        if (!"pending".equals(o.getStatus()) && !"confirmed".equals(o.getStatus())) {
            throw new BusinessException(
                "Only pending / confirmed orders can be cancelled (current: " + o.getStatus() + ")");
        }
        o.setStatus("cancelled");
        orderMapper.updateById(o);
        log.info("[Order cancelled] code={}", o.getCode());
    }

    /** 软删 (仅 pending / cancelled 可删) */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SalesOrder o = orderMapper.selectById(id);
        if (o == null) throw new BusinessException(R.NOT_FOUND, "Order not found");
        if (!"pending".equals(o.getStatus()) && !"cancelled".equals(o.getStatus())) {
            throw new BusinessException("Only pending / cancelled orders can be deleted");
        }
        orderMapper.deleteById(id);
    }

    // ============================================================
    // 内部工具
    // ============================================================
    private void validateCustomer(Long customerId) {
        Customer c = customerMapper.selectById(customerId);
        if (c == null) throw new BusinessException("Customer not found: " + customerId);
        if (!"active".equals(c.getStatus())) {
            throw new BusinessException("Customer is inactive: " + c.getName());
        }
    }

    private void validateAndEnrichItems(List<SalesOrderForm.Item> items) {
        for (SalesOrderForm.Item it : items) {
            Sku sku = skuMapper.selectById(it.getSkuId());
            if (sku == null) {
                throw new BusinessException("SKU not found: " + it.getSkuId());
            }
        }
    }

    /** 插入 items, 返回总金额 */
    private BigDecimal insertItems(Long orderId, List<SalesOrderForm.Item> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (SalesOrderForm.Item it : items) {
            BigDecimal amount = it.getQty().multiply(it.getUnitPrice());
            OrderItem oi = new OrderItem();
            oi.setOrderId(orderId);
            oi.setSkuId(it.getSkuId());
            oi.setQty(it.getQty());
            oi.setUnitPrice(it.getUnitPrice());
            oi.setAmount(amount);
            oi.setQtyShipped(BigDecimal.ZERO);
            oi.setRemark(it.getRemark());
            itemMapper.insert(oi);
            total = total.add(amount);
        }
        return total;
    }
}
