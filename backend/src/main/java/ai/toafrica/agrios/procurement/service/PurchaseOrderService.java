package ai.toafrica.agrios.procurement.service;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.procurement.dto.PurchaseOrderForm;
import ai.toafrica.agrios.procurement.entity.PurchaseOrder;
import ai.toafrica.agrios.procurement.entity.PurchaseOrderItem;
import ai.toafrica.agrios.procurement.entity.Supplier;
import ai.toafrica.agrios.procurement.mapper.PurchaseOrderItemMapper;
import ai.toafrica.agrios.procurement.mapper.PurchaseOrderMapper;
import ai.toafrica.agrios.procurement.mapper.SupplierMapper;
import ai.toafrica.agrios.procurement.vo.PurchaseOrderDetailVO;
import ai.toafrica.agrios.procurement.vo.PurchaseOrderItemVO;
import ai.toafrica.agrios.procurement.vo.PurchaseOrderVO;
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
 * 采购订单服务 - Sprint 17.3.
 *
 *   状态机:
 *     draft → confirmed → received / partial_received
 *                     ↘ cancelled
 *
 *   create():
 *     - 自动生成 code = PO-YYYYMMDD-NNNN
 *     - 自动算 dueDate = orderDate + supplier.creditDays
 *     - 累加 items 算 totalAmount
 *
 *   confirm() / cancel() / markReceived(): 状态推进
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final PurchaseOrderMapper orderMapper;
    private final PurchaseOrderItemMapper itemMapper;
    private final SupplierMapper supplierMapper;

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Set<String> EDITABLE_STATUSES = Set.of("draft", "confirmed");

    // ============================================================
    // 列表
    // ============================================================
    public PageResult<PurchaseOrderVO> page(Long supplierId, String status, String code,
                                            LocalDate dateFrom, LocalDate dateTo,
                                            PageQuery pq) {
        QueryWrapper<PurchaseOrderVO> q = new QueryWrapper<>();
        q.isNull("o.deleted_at");
        if (supplierId != null) q.eq("o.supplier_id", supplierId);
        if (status != null && !status.isBlank()) q.eq("o.status", status.trim());
        if (code != null && !code.isBlank()) q.like("o.code", code.trim());
        if (dateFrom != null) q.ge("o.order_date", dateFrom);
        if (dateTo != null) q.le("o.order_date", dateTo);
        q.orderByDesc("o.order_date").orderByDesc("o.id");

        Page<PurchaseOrderVO> p = new Page<>(pq.getPage(), pq.getSize());
        return PageResult.of(orderMapper.pageWithJoin(p, q));
    }

    // ============================================================
    // 详情
    // ============================================================
    public PurchaseOrderDetailVO detail(Long id) {
        QueryWrapper<PurchaseOrderVO> q = new QueryWrapper<>();
        q.isNull("o.deleted_at");
        q.eq("o.id", id);
        Page<PurchaseOrderVO> p = new Page<>(1, 1);
        var pageData = orderMapper.pageWithJoin(p, q);
        if (pageData.getRecords().isEmpty()) {
            throw new BusinessException(R.NOT_FOUND, "Purchase order not found");
        }
        List<PurchaseOrderItemVO> items = itemMapper.findByPoId(id);
        PurchaseOrderDetailVO vo = new PurchaseOrderDetailVO();
        vo.setOrder(pageData.getRecords().get(0));
        vo.setItems(items);
        return vo;
    }

    // ============================================================
    // 创建 (draft)
    // ============================================================
    @Transactional(rollbackFor = Exception.class)
    public Long create(PurchaseOrderForm form) {
        Supplier supplier = validateSupplier(form.getSupplierId());

        int seq = orderMapper.countByDate(form.getOrderDate()) + 1;
        String code = String.format("PO-%s-%04d", form.getOrderDate().format(YMD), seq);

        PurchaseOrder o = new PurchaseOrder();
        o.setCode(code);
        o.setSupplierId(form.getSupplierId());
        o.setOrderDate(form.getOrderDate());
        o.setExpectedDate(form.getExpectedDate());
        o.setCurrency(form.getCurrency());
        o.setFxRate(form.getFxRate() != null ? form.getFxRate() : BigDecimal.ONE);
        o.setStatus("draft");
        o.setPaymentStatus("unpaid");
        o.setPaidAmount(BigDecimal.ZERO);

        int creditDays = supplier.getCreditDays() != null ? supplier.getCreditDays() : 0;
        o.setDueDate(form.getOrderDate().plusDays(creditDays));

        o.setRemark(form.getRemark());
        o.setTotalAmount(BigDecimal.ZERO);
        orderMapper.insert(o);

        BigDecimal total = insertItems(o.getId(), form.getItems());
        o.setTotalAmount(total);
        orderMapper.updateById(o);

        log.info("[PO created] code={} supplier={} total={} {}", code, supplier.getName(),
                total.toPlainString(), form.getCurrency());
        return o.getId();
    }

    // ============================================================
    // 编辑 (仅 draft / confirmed)
    // ============================================================
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, PurchaseOrderForm form) {
        PurchaseOrder o = orderMapper.selectById(id);
        if (o == null) throw new BusinessException(R.NOT_FOUND, "Purchase order not found");
        if (!EDITABLE_STATUSES.contains(o.getStatus())) {
            throw new BusinessException("Cannot edit PO in status: " + o.getStatus());
        }
        validateSupplier(form.getSupplierId());

        o.setSupplierId(form.getSupplierId());
        o.setOrderDate(form.getOrderDate());
        o.setExpectedDate(form.getExpectedDate());
        o.setCurrency(form.getCurrency());
        o.setFxRate(form.getFxRate() != null ? form.getFxRate() : BigDecimal.ONE);
        o.setRemark(form.getRemark());

        itemMapper.delete(new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getPoId, id));
        BigDecimal total = insertItems(id, form.getItems());
        o.setTotalAmount(total);
        orderMapper.updateById(o);
    }

    // ============================================================
    // 状态机
    // ============================================================
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long id) {
        PurchaseOrder o = orderMapper.selectById(id);
        if (o == null) throw new BusinessException(R.NOT_FOUND, "Purchase order not found");
        if (!"draft".equals(o.getStatus())) {
            throw new BusinessException("Only draft POs can be confirmed (current: " + o.getStatus() + ")");
        }
        o.setStatus("confirmed");
        orderMapper.updateById(o);
        log.info("[PO confirmed] code={}", o.getCode());
    }

    /** 全部收货 (简化版: 不区分明细数量, 一键 received) */
    @Transactional(rollbackFor = Exception.class)
    public void markReceived(Long id) {
        PurchaseOrder o = orderMapper.selectById(id);
        if (o == null) throw new BusinessException(R.NOT_FOUND, "Purchase order not found");
        if (!"confirmed".equals(o.getStatus()) && !"partial_received".equals(o.getStatus())) {
            throw new BusinessException("Only confirmed / partial_received POs can be marked received (current: " + o.getStatus() + ")");
        }
        // 把所有 items 的 received_qty 同步到 quantity
        List<PurchaseOrderItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getPoId, id));
        for (PurchaseOrderItem it : items) {
            it.setReceivedQty(it.getQuantity());
            itemMapper.updateById(it);
        }
        o.setStatus("received");
        orderMapper.updateById(o);
        log.info("[PO received] code={}", o.getCode());
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        PurchaseOrder o = orderMapper.selectById(id);
        if (o == null) throw new BusinessException(R.NOT_FOUND, "Purchase order not found");
        if (!"draft".equals(o.getStatus()) && !"confirmed".equals(o.getStatus())) {
            throw new BusinessException("Only draft / confirmed POs can be cancelled (current: " + o.getStatus() + ")");
        }
        o.setStatus("cancelled");
        orderMapper.updateById(o);
        log.info("[PO cancelled] code={}", o.getCode());
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        PurchaseOrder o = orderMapper.selectById(id);
        if (o == null) throw new BusinessException(R.NOT_FOUND, "Purchase order not found");
        if (!"draft".equals(o.getStatus()) && !"cancelled".equals(o.getStatus())) {
            throw new BusinessException("Only draft / cancelled POs can be deleted");
        }
        orderMapper.deleteById(id);
    }

    // ============================================================
    // 内部工具
    // ============================================================
    private Supplier validateSupplier(Long supplierId) {
        Supplier s = supplierMapper.selectById(supplierId);
        if (s == null) throw new BusinessException("Supplier not found: " + supplierId);
        if (!"active".equals(s.getStatus())) {
            throw new BusinessException("Supplier is inactive: " + s.getName());
        }
        return s;
    }

    private BigDecimal insertItems(Long poId, List<PurchaseOrderForm.Item> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseOrderForm.Item it : items) {
            BigDecimal amount = it.getQuantity().multiply(it.getUnitPrice());
            PurchaseOrderItem poi = new PurchaseOrderItem();
            poi.setPoId(poId);
            poi.setInputType(it.getInputType());
            poi.setDescription(it.getDescription());
            poi.setQuantity(it.getQuantity());
            poi.setUnit(it.getUnit());
            poi.setUnitPrice(it.getUnitPrice());
            poi.setAmount(amount);
            poi.setReceivedQty(BigDecimal.ZERO);
            poi.setRemark(it.getRemark());
            itemMapper.insert(poi);
            total = total.add(amount);
        }
        return total;
    }
}
