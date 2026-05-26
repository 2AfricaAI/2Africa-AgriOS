package ai.toafrica.agrios.finance.service.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Africa's Talking SMS Provider - 真实接入预留 (Sprint 16.8 stub).
 *
 * <p>启用方式: application.yml 设置:
 * <pre>
 * sms:
 *   provider: africas_talking
 *   africas-talking:
 *     username: ${AT_USERNAME}     # sandbox or live
 *     api-key:  ${AT_API_KEY}
 *     sender:   2AFRICA            # 短信发送方显示名
 * </pre>
 *
 * <p>TODO[Sprint 17+]: 用 OkHttp 调 Africa's Talking REST API
 *   POST https://api.africastalking.com/version1/messaging
 *   form-data: username, to (E.164), message, from (sender)
 *   返回: SMSMessageData.Recipients[].messageId / status
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sms.provider", havingValue = "africas_talking")
public class AfricasTalkingProvider implements SmsProvider {

    @Value("${sms.africas-talking.username:}") private String username;
    @Value("${sms.africas-talking.api-key:}")  private String apiKey;
    @Value("${sms.africas-talking.sender:2AFRICA}") private String sender;

    @Override
    public String name() { return "africas_talking"; }

    @Override
    public SmsResult send(String channel, String phone, String content) {
        // TODO: 实际调用 Africa's Talking API
        log.warn("[AT-Provider] not yet implemented (config: username={}, sender={}). Falling back to log-only.",
                username, sender);
        log.info("[AT-Provider] would send {} → {}: {}", channel, phone, content);
        return SmsResult.fail("AfricasTalkingProvider not yet implemented");
    }
}
