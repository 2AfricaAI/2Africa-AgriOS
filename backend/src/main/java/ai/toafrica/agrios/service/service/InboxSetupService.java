package ai.toafrica.agrios.service.service;

import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.service.client.ChatwootClient;
import ai.toafrica.agrios.service.client.dto.ChatwootInbox;
import ai.toafrica.agrios.service.config.AfricasTalkingProperties;
import ai.toafrica.agrios.service.config.ChatwootProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * "Idiot-proof" inbox setup. Takes a small, opinionated user-facing form
 * payload (email + App Password, or WhatsApp Cloud creds, or web widget
 * settings), fills in every Chatwoot field the platform expects, and POSTs
 * to {@code /inboxes}.
 *
 * <p>Sprint 42 v0.1 — Email, WhatsApp Cloud and Web Widget. Sprint 43 will
 * add Twitter / Facebook / Slack / TikTok / Telegram (OAuth-flow channels).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InboxSetupService {

    private final ChatwootClient chatwoot;
    private final AfricasTalkingProperties atProps;
    private final ChatwootProperties cwProps;

    // =======================================================================
    // Email — IMAP/SMTP. The wizard form asks the operator for the bare
    // minimum: address + App Password. We auto-detect the IMAP/SMTP host for
    // the major providers (Gmail, Microsoft 365, iCloud, Yahoo) so the wizard
    // doesn't expose port numbers and TLS toggles.
    // =======================================================================

    public ChatwootInbox setupEmail(EmailSetupRequest req) {
        if (req == null || req.getEmail() == null || req.getEmail().isBlank()) {
            throw new BusinessException("email is required");
        }
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new BusinessException("password (app password) is required");
        }

        MailProvider provider = MailProvider.detect(req.getEmail(), req.getImapAddress());

        Map<String, Object> channel = new HashMap<>();
        channel.put("type", "email");
        channel.put("email", req.getEmail());
        if (req.getForwardToEmail() != null && !req.getForwardToEmail().isBlank()) {
            channel.put("forward_to_email", req.getForwardToEmail());
        }

        // SMTP (outbound)
        channel.put("smtp_enabled", true);
        channel.put("smtp_address", firstNonBlank(req.getSmtpAddress(), provider.smtpHost));
        channel.put("smtp_port", req.getSmtpPort() != null ? req.getSmtpPort() : provider.smtpPort);
        channel.put("smtp_login", firstNonBlank(req.getSmtpLogin(), req.getEmail()));
        channel.put("smtp_password", firstNonBlank(req.getSmtpPassword(), req.getPassword()));
        channel.put("smtp_domain", firstNonBlank(req.getSmtpDomain(), domainOf(req.getEmail())));
        channel.put("smtp_authentication", "login");
        channel.put("smtp_enable_starttls_auto", true);
        channel.put("smtp_openssl_verify_mode", "none");

        // IMAP (inbound)
        channel.put("imap_enabled", true);
        channel.put("imap_address", firstNonBlank(req.getImapAddress(), provider.imapHost));
        channel.put("imap_port", req.getImapPort() != null ? req.getImapPort() : provider.imapPort);
        channel.put("imap_login", firstNonBlank(req.getImapLogin(), req.getEmail()));
        channel.put("imap_password", firstNonBlank(req.getImapPassword(), req.getPassword()));
        channel.put("imap_enable_ssl", true);

        Map<String, Object> body = new HashMap<>();
        body.put("name", firstNonBlank(req.getName(), provider.label + " Email"));
        body.put("channel", channel);

        ChatwootInbox out = chatwoot.createInbox(body);
        log.info("[InboxSetup] email inbox created: id={} name={} provider={}",
                out.getId(), out.getName(), provider);
        return out;
    }

    // =======================================================================
    // WhatsApp Cloud (Meta official). Operator pre-configures a phone number
    // in Meta Business Manager, then pastes the three identifiers here.
    // =======================================================================

    public ChatwootInbox setupWhatsApp(WhatsAppSetupRequest req) {
        if (req == null) throw new BusinessException("payload is required");
        if (blank(req.getPhoneNumber())) throw new BusinessException("phoneNumber is required");
        if (blank(req.getApiToken()))    throw new BusinessException("apiToken (Meta) is required");
        if (blank(req.getPhoneNumberId()))    throw new BusinessException("phoneNumberId (Meta) is required");
        if (blank(req.getBusinessAccountId())) throw new BusinessException("businessAccountId (Meta) is required");

        Map<String, Object> providerConfig = new HashMap<>();
        providerConfig.put("api_key", req.getApiToken());
        providerConfig.put("phone_number_id", req.getPhoneNumberId());
        providerConfig.put("business_account_id", req.getBusinessAccountId());

        Map<String, Object> channel = new HashMap<>();
        channel.put("type", "whatsapp");
        channel.put("phone_number", normalizePhone(req.getPhoneNumber()));
        channel.put("provider", "whatsapp_cloud");
        channel.put("provider_config", providerConfig);

        Map<String, Object> body = new HashMap<>();
        body.put("name", firstNonBlank(req.getName(), "WhatsApp " + req.getPhoneNumber()));
        body.put("channel", channel);

        ChatwootInbox out = chatwoot.createInbox(body);
        log.info("[InboxSetup] WhatsApp Cloud inbox created: id={} phone={}", out.getId(), req.getPhoneNumber());
        return out;
    }

    // =======================================================================
    // SMS via Africa's Talking. We create a Chatwoot "API Channel" inbox and
    // point its outgoing webhook back at AgriOS — so when an agent replies in
    // the Chatwoot UI, our backend gets the message and relays it through
    // Africa's Talking. Inbound SMS comes in via the Africa's Talking
    // dashboard webhook (configured by the operator after this returns) and
    // hits {@code /v1/service/webhook/africastalking/incoming}.
    //
    // Credentials are global (one AT account per AgriOS install). The
    // wizard records the resulting Chatwoot inbox id back into
    // {@link AfricasTalkingProperties} so subsequent webhook traffic can
    // identify the right channel.
    // =======================================================================

    public SmsSetupResult setupSms(SmsSetupRequest req) {
        if (req == null) throw new BusinessException("payload is required");
        if (blank(req.getUsername()))  throw new BusinessException("Africa's Talking username is required");
        if (blank(req.getApiKey()))    throw new BusinessException("Africa's Talking API key is required");

        // Persist into the in-memory props bean so subsequent webhook traffic
        // can read them. Operators should still set the env vars for restart
        // durability — the wizard surfaces those values in the result.
        atProps.setUsername(req.getUsername());
        atProps.setApiKey(req.getApiKey());
        atProps.setSenderId(req.getSenderId() == null ? "" : req.getSenderId());
        atProps.setSandbox(req.isSandbox());

        String outboundWebhook = req.getPublicAgriosUrl() != null && !req.getPublicAgriosUrl().isBlank()
                ? req.getPublicAgriosUrl()
                : "http://host.docker.internal:8080";
        outboundWebhook = stripTrailingSlash(outboundWebhook)
                + "/api/v1/service/webhook/africastalking/outbound";

        Map<String, Object> channel = new HashMap<>();
        channel.put("type", "api");
        channel.put("webhook_url", outboundWebhook);

        Map<String, Object> body = new HashMap<>();
        body.put("name", firstNonBlank(req.getName(), "SMS via Africa's Talking"));
        body.put("channel", channel);

        ChatwootInbox out = chatwoot.createInbox(body);
        atProps.setSmsInboxId(out.getId());

        // Compose the inbound webhook the operator must paste into the
        // Africa's Talking dashboard ("SMS Callback URL" for inbound).
        String publicBase = stripTrailingSlash(req.getPublicAgriosUrl());
        String inboundWebhook = (publicBase == null || publicBase.isBlank())
                ? "https://<your-public-agrios-url>/api/v1/service/webhook/africastalking/incoming"
                : publicBase + "/api/v1/service/webhook/africastalking/incoming";

        log.info("[InboxSetup] SMS inbox created: id={} sandbox={} inboundWebhook={}",
                out.getId(), req.isSandbox(), inboundWebhook);

        return SmsSetupResult.builder()
                .inbox(out)
                .inboundWebhookUrl(inboundWebhook)
                .outboundWebhookUrl(outboundWebhook)
                .sandbox(req.isSandbox())
                .build();
    }

    private static String stripTrailingSlash(String s) {
        if (s == null) return null;
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    // =======================================================================
    // Web widget — easiest channel, no external credentials.
    // =======================================================================

    public ChatwootInbox setupWebWidget(WebWidgetSetupRequest req) {
        if (req == null) throw new BusinessException("payload is required");
        if (blank(req.getWebsiteUrl())) throw new BusinessException("websiteUrl is required");

        Map<String, Object> channel = new HashMap<>();
        channel.put("type", "web_widget");
        channel.put("website_url", req.getWebsiteUrl());
        channel.put("welcome_title", firstNonBlank(req.getWelcomeTitle(), "Hi there!"));
        channel.put("welcome_tagline", firstNonBlank(req.getWelcomeTagline(), "We're here to help."));
        channel.put("widget_color", firstNonBlank(req.getWidgetColor(), "#0F3A26")); // AgriOS green
        channel.put("reply_time", "in_a_few_minutes");

        Map<String, Object> body = new HashMap<>();
        body.put("name", firstNonBlank(req.getName(), "Website Chat"));
        body.put("channel", channel);

        ChatwootInbox out = chatwoot.createInbox(body);
        log.info("[InboxSetup] WebWidget inbox created: id={} name={}", out.getId(), out.getName());
        return out;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static boolean blank(String s) { return s == null || s.isBlank(); }

    private static String firstNonBlank(String a, String b) {
        return blank(a) ? b : a;
    }

    private static String domainOf(String email) {
        if (email == null) return "";
        int i = email.indexOf('@');
        return i < 0 ? email : email.substring(i + 1);
    }

    /** Strip whitespace / dashes / parens; ensure leading + for E.164. */
    static String normalizePhone(String raw) {
        if (raw == null) return null;
        String s = raw.replaceAll("[\\s\\-()]", "");
        if (s.isEmpty()) return null;
        if (!s.startsWith("+")) {
            if (s.startsWith("00")) s = "+" + s.substring(2);
            else if (s.startsWith("254")) s = "+" + s;
            else if (s.startsWith("0") && s.length() == 10) s = "+254" + s.substring(1);
            else s = "+" + s;
        }
        return s;
    }

    /**
     * Mail provider auto-detection. Looks at the email's domain (and an
     * optional explicit override) to pick the right IMAP/SMTP hosts and
     * ports. Falls back to {@link #OTHER} which means "operator must fill
     * the advanced fields by hand".
     */
    public enum MailProvider {
        GMAIL ("Gmail",       "imap.gmail.com",        993, "smtp.gmail.com",        587),
        MS365 ("Microsoft 365", "outlook.office365.com", 993, "smtp.office365.com",   587),
        ICLOUD("iCloud",      "imap.mail.me.com",      993, "smtp.mail.me.com",      587),
        YAHOO ("Yahoo",       "imap.mail.yahoo.com",   993, "smtp.mail.yahoo.com",   587),
        OTHER ("Other",       null,                    993, null,                    587);

        public final String label;
        public final String imapHost;
        public final int    imapPort;
        public final String smtpHost;
        public final int    smtpPort;

        MailProvider(String label, String imapHost, int imapPort, String smtpHost, int smtpPort) {
            this.label = label;
            this.imapHost = imapHost;
            this.imapPort = imapPort;
            this.smtpHost = smtpHost;
            this.smtpPort = smtpPort;
        }

        public static MailProvider detect(String email, String imapOverride) {
            // If the operator passed an explicit imap_address, we trust them
            // and return OTHER so we don't override what they typed.
            if (imapOverride != null && !imapOverride.isBlank()) return OTHER;
            if (email == null) return OTHER;
            String d = email.toLowerCase(Locale.ROOT);
            int at = d.indexOf('@');
            if (at < 0) return OTHER;
            String dom = d.substring(at + 1);
            if (dom.endsWith("gmail.com") || dom.endsWith("googlemail.com")) return GMAIL;
            if (dom.endsWith("outlook.com") || dom.endsWith("hotmail.com")
                    || dom.endsWith("live.com") || dom.endsWith("office365.com")) return MS365;
            if (dom.endsWith("icloud.com") || dom.endsWith("me.com")) return ICLOUD;
            if (dom.endsWith("yahoo.com") || dom.endsWith("ymail.com")) return YAHOO;
            return OTHER;
        }
    }

    // -----------------------------------------------------------------------
    // Request DTOs — kept as nested static classes so the controller can
    // bind from JSON without a separate dto/ folder.
    // -----------------------------------------------------------------------

    @lombok.Data
    public static class EmailSetupRequest {
        private String name;
        private String email;
        /** App password / IMAP+SMTP password (most common case). */
        private String password;
        private String forwardToEmail;

        // Optional advanced overrides
        private String smtpAddress;
        private Integer smtpPort;
        private String smtpLogin;
        private String smtpPassword;
        private String smtpDomain;
        private String imapAddress;
        private Integer imapPort;
        private String imapLogin;
        private String imapPassword;
    }

    @lombok.Data
    public static class WhatsAppSetupRequest {
        private String name;
        private String phoneNumber;
        private String apiToken;
        private String phoneNumberId;
        private String businessAccountId;
    }

    @lombok.Data
    public static class WebWidgetSetupRequest {
        private String name;
        private String websiteUrl;
        private String welcomeTitle;
        private String welcomeTagline;
        /** Hex color, e.g. "#0F3A26". */
        private String widgetColor;
    }

    @lombok.Data
    public static class SmsSetupRequest {
        private String name;
        private String username;
        private String apiKey;
        /** Optional alphanumeric / short code sender id. */
        private String senderId;
        /** True for the AT sandbox (no real billing, +254 test numbers only). */
        private boolean sandbox = true;
        /**
         * Public URL the operator's AgriOS install is reachable at. Used to
         * compose the inbound + outbound webhook URLs the wizard surfaces
         * back to the operator. Optional during dev (we fall back to
         * host.docker.internal for outbound).
         */
        private String publicAgriosUrl;
    }

    @lombok.Data
    @lombok.Builder
    public static class SmsSetupResult {
        private ChatwootInbox inbox;
        private String inboundWebhookUrl;
        private String outboundWebhookUrl;
        private boolean sandbox;
    }
}
