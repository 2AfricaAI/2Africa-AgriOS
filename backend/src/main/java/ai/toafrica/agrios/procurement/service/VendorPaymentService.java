package ai.toafrica.agrios.procurement.service;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.procurement.dto.VendorPaymentForm;
import ai.toafrica.agrios.procurement.entity.PurchaseOrder;
import ai.toafrica.agrios.procurement.entity.VendorPayment;
import ai.toafrica.agrios.procurement.mapper.PurchaseOrderMapper;
import ai.toafrica.agrios.procurement.mapper.VendorPaymentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * VendorPayment 业务服务 - Sprint 17.5 (镜像 PaymentService).
 *   create():
 *     - 生成 code VPAY-YYYYMMDD-NNNN
 *     - amountKes = amount × fxRate
 *     - status = cleared
 *     - 级联刷新 PO.paid_amount + payment_status
 *
 *   delete() = 软删 (status → reversed) + 级联刷新
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VendorPaymentService {

    private final VendorPaymentMapper paymentMapper;
    private final PurchaseOrderMapper orderMapper;

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    public PageResult<VendorPayment> page(Long poId, Long supplierId, String method, PageQuery pq) {
        LambdaQueryWrapper<VendorPayment> q = new LambdaQueryWrapper<>();
        if (poId != null) q.eq(VendorPayment::getPoId, poId);
        if (supplierId != null) q.eq(VendorPayment::getSupplierId, supplierId);
        if (method != null && !method.isBlank()) q.eq(VendorPayment::getMethod, method.trim());
        q.orderByDesc(VendorPayment::getPaymentDate).orderByDesc(VendorPayment::getId);
        Page<VendorPayment> p = new Page<>(pq.getPage(), pq.getSize());
        return PageResult.of(paymentMapper.selectPage(p, q));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(VendorPaymentForm form) {
        PurchaseOrder po = orderMapper.selectById(form.getPoId());
        if (po == null) throw new BusinessException(R.NOT_FOUND, "Purchase order not found");

        BigDecimal fxRate = form.getFxRate() != null ? form.getFxRate() : BigDecimal.ONE;
        BigDecimal amountKes = form.getAmount().multiply(fxRate);

        VendorPayment vp = new VendorPayment();
        int seq = (int) (paymentMapper.selectCount(null) + 1);
        vp.setCode(String.format("VPAY-%s-%04d", LocalDate.now().format(YMD), seq));
        vp.setPoId(form.getPoId());
        vp.setSupplierId(po.getSupplierId());
        vp.setAmount(form.getAmount());
        vp.setCurrency(form.getCurrency() != null ? form.getCurrency() : po.getCurrency());
        vp.setFxRate(fxRate);
        vp.setAmountKes(amountKes);
        vp.setMethod(form.getMethod());
        vp.setPaymentDate(form.getPaymentDate());
        vp.setReferenceNo(form.getReferenceNo());
        vp.setPosTerminalId(form.getPosTerminalId());
        vp.setChannel(form.getChannel());
        vp.setStatus("cleared");
        vp.setRemark(form.getRemark());
        paymentMapper.insert(vp);

        refreshPoPayment(po);

        log.info("[VendorPayment] po={} amount={} {} method={} ref={} → paid={} status={}",
                po.getCode(), form.getAmount(), vp.getCurrency(),
                form.getMethod(), form.getReferenceNo(),
                po.getPaidAmount(), po.getPaymentStatus());

        return vp.getId();
    }

    /**
     * 重新计算 PO.paid_amount + payment_status.
     */
    private void refreshPoPayment(PurchaseOrder po) {
        BigDecimal paid = paymentMapper.sumByPo(po.getId());
        if (paid == null) paid = BigDecimal.ZERO;
        po.setPaidAmount(paid);
        if (paid.compareTo(BigDecimal.ZERO) <= 0) {
            po.setPaymentStatus("unpaid");
        } else if (paid.compareTo(po.getTotalAmount()) >= 0) {
            po.setPaymentStatus("paid");
        } else {
            po.setPaymentStatus("partial");
        }
        orderMapper.updateById(po);
    }

    public void delete(Long id) {
        VendorPayment vp = paymentMapper.selectById(id);
        if (vp == null) throw new BusinessException(R.NOT_FOUND, "Vendor payment not found");
        // 软删 - 改 status=reversed
        vp.setStatus("reversed");
        paymentMapper.updateById(vp);
        PurchaseOrder po = orderMapper.selectById(vp.getPoId());
        if (po != null) refreshPoPayment(po);
        log.info("[VendorPayment reversed] code={} amount={} ref={}", vp.getCode(), vp.getAmount(), vp.getReferenceNo());
    }
}
