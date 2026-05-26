package ai.toafrica.agrios.finance.service;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.finance.dto.PaymentForm;
import ai.toafrica.agrios.finance.entity.Payment;
import ai.toafrica.agrios.finance.mapper.PaymentMapper;
import ai.toafrica.agrios.sales.entity.SalesOrder;
import ai.toafrica.agrios.sales.mapper.SalesOrderMapper;
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
 * Payment 业务服务 - Sprint 14.2.
 * MVP: 手工录回款 (cash / bank / cheque / m-pesa-manual)
 * Sprint 15 会加 M-Pesa Daraja 自动回调写入.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentMapper paymentMapper;
    private final SalesOrderMapper orderMapper;

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    public PageResult<Payment> page(Long orderId, Long customerId, String method, PageQuery pq) {
        LambdaQueryWrapper<Payment> q = new LambdaQueryWrapper<>();
        if (orderId != null) q.eq(Payment::getOrderId, orderId);
        if (customerId != null) q.eq(Payment::getCustomerId, customerId);
        if (method != null && !method.isBlank()) q.eq(Payment::getMethod, method.trim());
        q.orderByDesc(Payment::getPaymentDate).orderByDesc(Payment::getId);
        Page<Payment> p = new Page<>(pq.getPage(), pq.getSize());
        return PageResult.of(paymentMapper.selectPage(p, q));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(PaymentForm form) {
        SalesOrder order = orderMapper.selectById(form.getOrderId());
        if (order == null) throw new BusinessException(R.NOT_FOUND, "Order not found");

        BigDecimal fxRate = form.getFxRate() != null ? form.getFxRate() : BigDecimal.ONE;
        BigDecimal amountKes = form.getAmount().multiply(fxRate);

        Payment p = new Payment();
        int seq = (int) (paymentMapper.selectCount(null) + 1);
        p.setCode(String.format("PAY-%s-%04d", LocalDate.now().format(YMD), seq));
        p.setOrderId(form.getOrderId());
        p.setCustomerId(order.getCustomerId());
        p.setAmount(form.getAmount());
        p.setCurrency(form.getCurrency() != null ? form.getCurrency() : order.getCurrency());
        p.setFxRate(fxRate);
        p.setAmountKes(amountKes);
        p.setMethod(form.getMethod());
        p.setPaymentDate(form.getPaymentDate());
        p.setReferenceNo(form.getReferenceNo());
        p.setPosTerminalId(form.getPosTerminalId());
        p.setChannel(form.getChannel());
        p.setStatus("cleared");
        p.setRemark(form.getRemark());
        paymentMapper.insert(p);

        // 级联刷新订单付款状态 (累加已收 + 更新 payment_status)
        refreshOrderPayment(order);

        log.info("[Payment] order={} amount={} {} method={} ref={} → paid={} status={}",
                order.getCode(), form.getAmount(), p.getCurrency(),
                form.getMethod(), form.getReferenceNo(),
                order.getPaidAmount(), order.getPaymentStatus());

        return p.getId();
    }

    /**
     * 重新计算 order.paid_amount + payment_status (累加所有 cleared+partial 的 payment 行)
     */
    private void refreshOrderPayment(SalesOrder order) {
        BigDecimal received = paymentMapper.sumByOrder(order.getId());
        if (received == null) received = BigDecimal.ZERO;
        order.setPaidAmount(received);
        if (received.compareTo(BigDecimal.ZERO) <= 0) {
            order.setPaymentStatus("unpaid");
        } else if (received.compareTo(order.getTotalAmount()) >= 0) {
            order.setPaymentStatus("paid");
        } else {
            order.setPaymentStatus("partial");
        }
        orderMapper.updateById(order);
    }

    public void delete(Long id) {
        Payment p = paymentMapper.selectById(id);
        if (p == null) throw new BusinessException(R.NOT_FOUND, "Payment not found");
        // 软删: 改 status=reversed 而不是物理删, 留审计痕迹
        p.setStatus("reversed");
        paymentMapper.updateById(p);
        // 级联刷新订单 (reversed payment 不再计入 received)
        SalesOrder order = orderMapper.selectById(p.getOrderId());
        if (order != null) refreshOrderPayment(order);
        log.info("[Payment reversed] code={} amount={} ref={}", p.getCode(), p.getAmount(), p.getReferenceNo());
    }
}
