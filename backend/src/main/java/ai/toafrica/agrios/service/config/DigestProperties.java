package ai.toafrica.agrios.service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;

/**
 * Sprint 50e -- weekly CS digest email configuration.
 *
 * <pre>
 *   AGRIOS_DIGEST_ENABLED=true
 *   AGRIOS_DIGEST_CRON=0 0 6 ? * MON       # Monday 06:00 server time
 *   AGRIOS_DIGEST_RECIPIENTS=ops@farm.example,cto@farm.example
 *   AGRIOS_DIGEST_FROM=cs-bot@farm.example
 *   AGRIOS_DIGEST_WINDOWDAYS=7
 *   AGRIOS_DIGEST_LOCALE=en
 *   AGRIOS_DIGEST_SUBJECTPREFIX=[2Africa CS Weekly]
 * </pre>
 *
 * <p>Spring's MailSender autoconfig reads {@code spring.mail.*} so the SMTP
 * host / port / username / password live there, not here.</p>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "agrios.digest")
public class DigestProperties {

    /** Master kill-switch. Defaults true so a fresh deploy still works. */
    private boolean enabled = true;

    /**
     * Spring cron expression for the @Scheduled trigger. Default: Monday 06:00.
     * Note: this is the 6-field Spring cron (seconds at the front), not crontab.
     */
    private String cron = "0 0 6 ? * MON";

    /** Comma-separated recipient list (read as List by Spring). */
    private List<String> recipients = Collections.emptyList();

    /** Optional CC list. */
    private List<String> cc = Collections.emptyList();

    /** Envelope from -- usually the same as spring.mail.username. */
    private String from = "cs-bot@localhost";

    /** Window in days the digest covers. 7 = last week. */
    private int windowDays = 7;

    /** Subject prefix; the rendered date range is appended. */
    private String subjectPrefix = "[2Africa CS Weekly]";

    /** Locale to render the email body in -- 'en' / 'zh' / 'sw'. */
    private String locale = "en";
}
