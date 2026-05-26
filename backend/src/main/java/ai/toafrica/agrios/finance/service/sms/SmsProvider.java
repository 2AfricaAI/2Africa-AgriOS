package ai.toafrica.agrios.finance.service.sms;

/**
 * SMS / WhatsApp 短信通道抽象 - Sprint 16.8.
 *   实现类:
 *     - StubSmsProvider          默认, 只打日志, 用于开发/CI
 *     - AfricasTalkingProvider   未来真实接入肯尼亚 Africa's Talking
 *     - TwilioProvider           未来真实接入 Twilio (跨境备份)
 *
 *   通过 application.yml 的 sms.provider 切换:
 *     sms.provider: stub | africas_talking | twilio
 */
public interface SmsProvider {

    /** Provider 唯一标识, 用于 sms_log.provider 字段 + application.yml 配置匹配 */
    String name();

    /**
     * 发送短信.
     * @param channel  sms / whatsapp
     * @param phone    +254... E.164 格式
     * @param content  已替换占位符的最终文本
     * @return 发送结果, 含 providerMsgId 和 success/失败
     */
    SmsResult send(String channel, String phone, String content);

    /**
     * 简单 DTO 返回结果.
     */
    record SmsResult(boolean success, String messageId, String error) {
        public static SmsResult ok(String messageId) {
            return new SmsResult(true, messageId, null);
        }
        public static SmsResult fail(String error) {
            return new SmsResult(false, null, error);
        }
    }
}
