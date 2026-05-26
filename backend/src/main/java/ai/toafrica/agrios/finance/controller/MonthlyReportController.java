package ai.toafrica.agrios.finance.controller;

import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.finance.service.LoopService;
import ai.toafrica.agrios.finance.service.MonthlyReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Tag(name = "52 · Finance Monthly + Loop",
     description = "Monthly P&L summary + Loop (NCBA) acquiring integration (stub pending merchant onboarding)")
@RestController
@RequestMapping("/v1/finance")
@RequiredArgsConstructor
public class MonthlyReportController {

    private final MonthlyReportService monthlyService;
    private final LoopService loopService;

    @Operation(summary = "Monthly P&L summary (last 12 months)")
    @GetMapping("/monthly")
    public R<List<Map<String, Object>>> monthly() {
        return R.ok(monthlyService.getMonthlySummary());
    }

    @Operation(summary = "Loop online checkout - create payment session (pending NCBA merchant onboarding)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or hasAuthority('ROLE_SALES')")
    @PostMapping("/loop/checkout")
    public R<String> createCheckout(@RequestBody Map<String, Object> body) {
        Long orderId = ((Number) body.get("orderId")).longValue();
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String currency = (String) body.getOrDefault("currency", "KES");
        String phone = (String) body.get("phone");
        return R.ok(loopService.createCheckout(orderId, amount, currency, phone));
    }

    /**
     * Loop webhook - Loop 在客户付款完成后回调此端点 (online + POS 共用).
     * 暂未启用 (waiting for Loop merchant onboarding).
     * 开通后:
     *   1. 把这个端点加到 SecurityConfig 的 permitAll 列表 (Loop 不带 JWT)
     *   2. 验证 Loop 签名 header
     *   3. 调 loopService.handleWebhook()
     */
    @Operation(summary = "Loop payment webhook (server-to-server, no auth) — handles both online + POS",
               description = "Pending NCBA Loop merchant API spec. Disabled in current build.")
    @PostMapping("/loop/webhook")
    public R<Void> loopWebhook(@RequestBody Map<String, Object> payload) {
        // TODO Sprint 15.x: validate Loop signature header, then:
        //   String loopTxId = (String) payload.get("transaction_id");
        //   Long orderId = ((Number) payload.get("order_ref")).longValue();
        //   BigDecimal amount = new BigDecimal(payload.get("amount").toString());
        //   String currency = (String) payload.get("currency");
        //   String channel  = (String) payload.get("channel");   // mpesa / card / bank
        //   String terminal = (String) payload.get("terminal_id"); // null for online
        //   loopService.handleWebhook(loopTxId, orderId, amount, currency, channel, terminal);
        return R.ok();
    }
}
