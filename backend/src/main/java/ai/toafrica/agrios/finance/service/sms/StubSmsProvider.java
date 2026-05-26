package ai.toafrica.agrios.finance.service.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 默认 SMS Provider - 仅打日志, 不真实发送.
 *   生产部署前接入 Africa's Talking / Twilio 时, 在 application.yml 改:
 *     sms.provider: africas_talking
 *   该 stub 即自动让位, 由 AfricasTalkingProvider 接管.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sms.provider", havingValue = "stub", matchIfMissing = true)
public class StubSmsProvider implements SmsProvider {

    @Override
    public String name() { return "stub"; }

    @Override
    public SmsResult send(String channel, String phone, String content) {
        String fakeId = "STUB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("┌─ [SMS-STUB] would send {} → {}", channel, phone);
        log.info("│  Content: {}", content);
        log.info("└─ msgId: {} (no real network call)", fakeId);
        return SmsResult.ok(fakeId);
    }
}
