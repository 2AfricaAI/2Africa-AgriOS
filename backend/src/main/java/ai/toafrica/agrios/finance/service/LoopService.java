package ai.toafrica.agrios.finance.service;

import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.finance.dto.PaymentForm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Loop (NCBA) 收单集成 - Sprint 15.1 框架 (stub).
 *
 * Loop 是统一的聚合收单方:
 *   - loop_online: web/app 端发起付款,客户在 Loop checkout 选 M-Pesa/Card/Bank
 *   - loop_pos:    门店 POS 机刷卡或扫码,实时通过 Loop 回传到本系统
 *
 * 当前状态: 接口框架就绪,等 Loop 商户开通 + API 文档接通.
 *
 * 正式实现要做的事 (上线前):
 *   1. 商户接入: 在 NCBA Loop 商户平台开 merchant account → 拿 client_id / client_secret / merchant_code
 *   2. 配置: application.yml 加 loop.base_url / loop.client_id / loop.client_secret / loop.callback_url
 *   3. 在线收单 (loop_online):
 *      - 后端调 POST /loop/v1/payments/checkout → 拿 checkout_url
 *      - 把 checkout_url 返给前端, 前端跳转 (或弹 iframe) 让客户付款
 *      - Loop 在客户付款完成后异步 POST 到 /api/v1/finance/loop/webhook
 *      - webhook 含 transaction_id / order_ref / amount / channel(mpesa/card/bank) / status
 *      - 我们写 payment 行 status=cleared, channel 填上实际通道
 *   4. POS 收单 (loop_pos):
 *      - POS 机本身与 Loop 后台通讯 (我们不直接对接 POS)
 *      - 每笔 POS 交易完成, Loop 回调同一个 webhook, payload 多带 terminal_id
 *      - 我们写 payment 行 method=loop_pos, pos_terminal_id 记录是哪台 POS 收的
 *   5. 对账:
 *      - 每日凌晨拉 Loop Statements API 与本地 payment 表比对
 *      - 差异 > KES 100 或笔数不一致进入异常队列
 *
 * 当前 stub:
 *   - createCheckout() 抛 BusinessException 提示 Loop 还没接通
 *   - handleWebhook() 已经按预期 payload 写好, 接通后只需要补 webhook endpoint
 *
 * 同时用户可以手工录 method=loop_online 或 loop_pos 的 payment,
 * 用 referenceNo 填 Loop 的 transaction_id, posTerminalId 填 POS 编号.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoopService {

    private final PaymentService paymentService;

    /**
     * 发起 Loop 在线付款,返回 checkout URL 让前端跳转.
     * TODO: 接 Loop merchant API /payments/checkout.
     */
    public String createCheckout(Long orderId, BigDecimal amount, String currency, String phone) {
        log.warn("[Loop] Online checkout attempted but Loop merchant API not yet configured. " +
                 "order={} amount={} {} phone={}", orderId, amount, currency, phone);
        throw new BusinessException(
            "Loop online checkout not yet enabled — pending NCBA Loop merchant onboarding. " +
            "Meanwhile, record Loop payments manually with method=loop_online and the Loop transaction_id in referenceNo.");
    }

    /**
     * Loop webhook 回调处理.
     * 不论是 loop_online (web checkout) 还是 loop_pos (门店刷卡), Loop 都用同一个 webhook 回传.
     * 通过 payload 中的 `source` 字段区分 (online / pos).
     *
     * @param loopTxId        Loop 内部交易号 (= payment.reference_no)
     * @param orderId         我们传给 Loop 作为 order_ref 的订单 ID
     * @param amount          金额 (原币)
     * @param currency        KES / USD / ...
     * @param channel         Loop 内部实际通道: mpesa / card / bank
     * @param posTerminalId   POS 机标识 (online 模式为 null)
     */
    public void handleWebhook(String loopTxId, Long orderId, BigDecimal amount,
                              String currency, String channel, String posTerminalId) {
        boolean isPos = posTerminalId != null && !posTerminalId.isBlank();
        log.info("[Loop webhook] tx={} order={} amount={} {} channel={} pos={} (method={})",
                loopTxId, orderId, amount, currency, channel, posTerminalId,
                isPos ? "loop_pos" : "loop_online");

        PaymentForm form = new PaymentForm();
        form.setOrderId(orderId);
        form.setAmount(amount);
        form.setCurrency(currency != null ? currency : "KES");
        form.setFxRate(BigDecimal.ONE);   // Loop 报告金额已是结算币
        form.setMethod(isPos ? "loop_pos" : "loop_online");
        form.setPaymentDate(LocalDate.now());
        form.setReferenceNo(loopTxId);
        form.setPosTerminalId(posTerminalId);
        form.setChannel(channel);
        form.setRemark("Auto-created from Loop webhook");
        paymentService.create(form);
    }
}
