package ai.toafrica.agrios.finance.controller;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.finance.dto.SmsSendForm;
import ai.toafrica.agrios.finance.entity.SmsLog;
import ai.toafrica.agrios.finance.entity.SmsTemplate;
import ai.toafrica.agrios.finance.mapper.SmsLogMapper;
import ai.toafrica.agrios.finance.service.SmsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "53 · Finance SMS", description = "SMS / WhatsApp templates + sending (Sprint 16.8)")
@RestController
@RequestMapping("/v1/finance/sms")
@RequiredArgsConstructor
public class SmsController {

    private final SmsService smsService;
    private final SmsLogMapper logMapper;

    @Operation(summary = "List enabled SMS templates")
    @GetMapping("/templates")
    public R<List<SmsTemplate>> templates() {
        return R.ok(smsService.listTemplates());
    }

    @Operation(summary = "Preview SMS — render template with customer/order placeholders (no send)")
    @PostMapping("/preview")
    public R<Map<String, String>> preview(@Valid @RequestBody SmsSendForm form) {
        String text = smsService.preview(form);
        return R.ok(Map.of("content", text));
    }

    @Operation(summary = "Send SMS / WhatsApp via configured provider")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGER') or hasAuthority('ROLE_SALES') or hasAuthority('ROLE_FINANCE')")
    @PostMapping("/send")
    public R<Long> send(@Valid @RequestBody SmsSendForm form) {
        return R.ok(smsService.send(form));
    }

    @Operation(summary = "List SMS send history (paginated, filterable by customer)")
    @GetMapping("/logs")
    public R<PageResult<SmsLog>> logs(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String status,
            PageQuery pq) {
        LambdaQueryWrapper<SmsLog> q = new LambdaQueryWrapper<>();
        if (customerId != null) q.eq(SmsLog::getCustomerId, customerId);
        if (orderId != null)    q.eq(SmsLog::getOrderId, orderId);
        if (status != null && !status.isBlank()) q.eq(SmsLog::getStatus, status.trim());
        q.orderByDesc(SmsLog::getSentAt).orderByDesc(SmsLog::getId);
        Page<SmsLog> p = new Page<>(pq.getPage(), pq.getSize());
        return R.ok(PageResult.of(logMapper.selectPage(p, q)));
    }
}
