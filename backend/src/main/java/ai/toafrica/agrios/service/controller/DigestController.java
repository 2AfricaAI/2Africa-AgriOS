package ai.toafrica.agrios.service.controller;

import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.service.service.WeeklyDigestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Sprint 50e -- manual triggers for the weekly CS digest.
 *
 * <p>The scheduled job already runs on Spring cron, but ops needs:</p>
 * <ul>
 *   <li>a way to look at the rendered HTML before forwarding to stakeholders</li>
 *   <li>a way to fire it now without waiting for Monday</li>
 * </ul>
 */
@Slf4j
@Tag(name = "94 - CS - Weekly Digest", description = "Weekly customer service digest email")
@RestController
@RequestMapping({"/v1/cs/analytics/digest", "/v1/service/analytics/digest"})
@RequiredArgsConstructor
public class DigestController {

    /**
     * Optional because the bean only registers when
     * {@code agrios.digest.enabled=true}. If it is off, the endpoints
     * still respond with a helpful 503-style message instead of 500.
     */
    @Autowired(required = false)
    private WeeklyDigestService digestService;

    @Operation(summary = "Preview the digest HTML without sending")
    @GetMapping(value = "/preview", produces = MediaType.TEXT_HTML_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> preview() {
        if (digestService == null) {
            return ResponseEntity.status(503)
                    .body("<p>Digest service disabled (agrios.digest.enabled=false)</p>");
        }
        return ResponseEntity.ok(digestService.preview());
    }

    @Operation(summary = "Trigger an immediate digest send (SUPER_ADMIN only)")
    @PostMapping("/send-now")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public R<Map<String, Object>> sendNow() {
        if (digestService == null) {
            throw new IllegalStateException(
                    "Digest service disabled (agrios.digest.enabled=false)");
        }
        List<String> sentTo = digestService.sendNow();
        return R.ok(Map.of(
                "sent", true,
                "recipients", sentTo
        ));
    }
}
