package ai.toafrica.agrios.service.controller;

import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.service.client.ChatwootClient;
import ai.toafrica.agrios.service.client.dto.ChatwootInbox;
import ai.toafrica.agrios.service.service.InboxSetupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sprint 42a — "idiot-proof" inbox setup endpoints. Each takes a tiny
 * user-facing form payload (email + App Password, WhatsApp Cloud creds, or
 * web widget settings) and fills in every Chatwoot field the platform needs.
 *
 * <p>The wizard UI at {@code /service/inboxes} drives these endpoints. A farm
 * owner never has to know what IMAP / STARTTLS / OAuth2 means.</p>
 */
@Tag(name = "92 · Service - Inbox Setup", description = "Channel onboarding wizards")
@RestController
@RequestMapping("/v1/service/inboxes")
@RequiredArgsConstructor
public class InboxSetupController {

    private final InboxSetupService setup;
    private final ChatwootClient chatwoot;

    @Operation(summary = "Create an Email inbox from a Gmail-style form (email + App Password)")
    @PostMapping("/setup-email")
    public R<ChatwootInbox> setupEmail(@RequestBody InboxSetupService.EmailSetupRequest req) {
        return R.ok(setup.setupEmail(req));
    }

    @Operation(summary = "Create a WhatsApp Cloud inbox (Meta Business Manager creds)")
    @PostMapping("/setup-whatsapp")
    public R<ChatwootInbox> setupWhatsApp(@RequestBody InboxSetupService.WhatsAppSetupRequest req) {
        return R.ok(setup.setupWhatsApp(req));
    }

    @Operation(summary = "Create a Web Widget inbox (no external credentials)")
    @PostMapping("/setup-web-widget")
    public R<ChatwootInbox> setupWebWidget(@RequestBody InboxSetupService.WebWidgetSetupRequest req) {
        return R.ok(setup.setupWebWidget(req));
    }

    @Operation(summary = "Create a SMS inbox via Africa's Talking (Kenya-friendly cheap SMS)")
    @PostMapping("/setup-sms")
    public R<InboxSetupService.SmsSetupResult> setupSms(@RequestBody InboxSetupService.SmsSetupRequest req) {
        return R.ok(setup.setupSms(req));
    }

    @Operation(summary = "Delete a Chatwoot inbox by id")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        chatwoot.deleteInbox(id);
        return R.ok();
    }
}
