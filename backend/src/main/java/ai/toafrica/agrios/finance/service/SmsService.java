package ai.toafrica.agrios.finance.service;

import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.finance.dto.SmsSendForm;
import ai.toafrica.agrios.finance.entity.SmsLog;
import ai.toafrica.agrios.finance.entity.SmsTemplate;
import ai.toafrica.agrios.finance.mapper.SmsLogMapper;
import ai.toafrica.agrios.finance.mapper.SmsTemplateMapper;
import ai.toafrica.agrios.finance.service.sms.SmsProvider;
import ai.toafrica.agrios.framework.security.SecurityUtil;
import ai.toafrica.agrios.sales.entity.Customer;
import ai.toafrica.agrios.sales.entity.SalesOrder;
import ai.toafrica.agrios.sales.mapper.CustomerMapper;
import ai.toafrica.agrios.sales.mapper.SalesOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SMS / WhatsApp 发送服务 - Sprint 16.8.
 *   - 模板查询
 *   - 上下文占位符替换 ({customerName} {orderCode} {amount} {currency} {dueDate} {daysOverdue})
 *   - 委派给 SmsProvider 真实发送
 *   - 入库 sms_log
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    private final SmsTemplateMapper templateMapper;
    private final SmsLogMapper logMapper;
    private final CustomerMapper customerMapper;
    private final SalesOrderMapper orderMapper;
    private final SmsProvider provider;       // Spring 按 sms.provider 配置注入

    public List<SmsTemplate> listTemplates() {
        return templateMapper.selectList(
                new LambdaQueryWrapper<SmsTemplate>()
                        .eq(SmsTemplate::getEnabled, 1)
                        .orderByAsc(SmsTemplate::getCode));
    }

    /**
     * 预览模板替换结果 (不真实发送, 用于前端确认前的预览)
     */
    public String preview(SmsSendForm form) {
        SmsTemplate tpl = mustTemplate(form.getTemplateCode());
        Customer c = mustCustomer(form.getCustomerId());
        SalesOrder o = form.getOrderId() != null ? orderMapper.selectById(form.getOrderId()) : null;
        return substitute(tpl.getContent(), buildContext(c, o));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long send(SmsSendForm form) {
        SmsTemplate tpl = mustTemplate(form.getTemplateCode());
        Customer c = mustCustomer(form.getCustomerId());
        SalesOrder o = form.getOrderId() != null ? orderMapper.selectById(form.getOrderId()) : null;

        String channel = form.getChannel() != null ? form.getChannel() : tpl.getChannel();
        String phone = form.getPhoneOverride() != null && !form.getPhoneOverride().isBlank()
                ? form.getPhoneOverride()
                : c.getContactPhone();

        if (phone == null || phone.isBlank()) {
            throw new BusinessException(R.BUSINESS_ERROR,
                    "Customer has no contact_phone and no phoneOverride provided");
        }

        String content = substitute(tpl.getContent(), buildContext(c, o));

        // 真实发送 (Stub 仅打日志)
        SmsProvider.SmsResult result = provider.send(channel, phone, content);

        // 入库日志
        SmsLog rec = new SmsLog();
        rec.setCustomerId(c.getId());
        rec.setOrderId(o != null ? o.getId() : null);
        rec.setTemplateCode(tpl.getCode());
        rec.setChannel(channel);
        rec.setPhone(phone);
        rec.setContent(content);
        rec.setProvider(provider.name());
        rec.setProviderMsgId(result.messageId());
        rec.setStatus(result.success() ? "sent" : "failed");
        rec.setError(result.error());
        rec.setOperatorId(SecurityUtil.currentUserId());
        logMapper.insert(rec);

        log.info("[SMS] template={} customer={} order={} channel={} provider={} status={} msgId={}",
                tpl.getCode(), c.getCode(),
                o != null ? o.getCode() : "-",
                channel, provider.name(), rec.getStatus(), result.messageId());

        if (!result.success()) {
            throw new BusinessException(R.BUSINESS_ERROR,
                    "SMS provider failed: " + result.error());
        }
        return rec.getId();
    }

    // ---------------------------------------------------------------------
    // 占位符上下文
    // ---------------------------------------------------------------------
    private Map<String, String> buildContext(Customer c, SalesOrder o) {
        Map<String, String> ctx = new HashMap<>();
        ctx.put("customerName", c.getName() != null ? c.getName() : "");
        if (o != null) {
            BigDecimal total = o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal paid  = o.getPaidAmount()  != null ? o.getPaidAmount()  : BigDecimal.ZERO;
            BigDecimal outstanding = total.subtract(paid).max(BigDecimal.ZERO);
            ctx.put("orderCode", o.getCode() != null ? o.getCode() : "");
            ctx.put("amount", String.format("%,.2f", outstanding));
            ctx.put("currency", o.getCurrency() != null ? o.getCurrency() : "KES");
            ctx.put("dueDate", o.getDueDate() != null ? o.getDueDate().toString() : "");
            if (o.getDueDate() != null) {
                long days = ChronoUnit.DAYS.between(o.getDueDate(), LocalDate.now());
                ctx.put("daysOverdue", String.valueOf(Math.max(days, 0)));
            } else {
                ctx.put("daysOverdue", "0");
            }
        } else {
            ctx.put("orderCode", "");
            ctx.put("amount", "");
            ctx.put("currency", "KES");
            ctx.put("dueDate", "");
            ctx.put("daysOverdue", "0");
        }
        return ctx;
    }

    /**
     * {key} 替换. 简单实现, 不支持嵌套或转义.
     */
    private static String substitute(String tpl, Map<String, String> ctx) {
        String out = tpl;
        for (Map.Entry<String, String> e : ctx.entrySet()) {
            out = out.replace("{" + e.getKey() + "}", e.getValue());
        }
        return out;
    }

    private SmsTemplate mustTemplate(String code) {
        SmsTemplate t = templateMapper.findByCode(code);
        if (t == null) {
            throw new BusinessException(R.NOT_FOUND, "SMS template not found: " + code);
        }
        return t;
    }
    private Customer mustCustomer(Long id) {
        Customer c = customerMapper.selectById(id);
        if (c == null) throw new BusinessException(R.NOT_FOUND, "Customer not found");
        return c;
    }
}
