package ai.toafrica.agrios.service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Sprint 50d -- CSAT survey configuration.
 *
 * <p>Env-driven, so a deployer can swap the public-facing URL host
 * without rebuilding the artifact:</p>
 *
 * <pre>
 *   AGRIOS_CSAT_PUBLICBASEURL=https://farm.example.com
 *   AGRIOS_CSAT_TTLDAYS=30
 * </pre>
 *
 * <p>Final survey URL shape:
 *   {@code <publicBaseUrl>/csat/{token}}</p>
 *
 * <p>If unset, falls back to a localhost dev URL so it just works on a
 * fresh checkout.</p>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "agrios.csat")
public class CsatProperties {

    /** Public-facing base URL for the survey page. No trailing slash. */
    private String publicBaseUrl = "http://localhost:5173";

    /** Token TTL in days; after this the link returns 410 Gone. */
    private int ttlDays = 30;

    /** Path prefix appended to publicBaseUrl. Default matches the Vue route. */
    private String path = "/csat/";
}
