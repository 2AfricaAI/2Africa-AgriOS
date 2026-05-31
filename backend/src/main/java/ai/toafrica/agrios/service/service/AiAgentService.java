package ai.toafrica.agrios.service.service;

import ai.toafrica.agrios.service.client.ChatwootClient;
import ai.toafrica.agrios.service.client.LlmClient;
import ai.toafrica.agrios.service.client.LlmRouter;
import ai.toafrica.agrios.service.config.ChatwootProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI Agent that reacts to inbound Chatwoot customer messages.
 *
 * <p>Sprint 40f v0.1:</p>
 * <ol>
 *   <li>Receive {@code message_created} webhook from Chatwoot</li>
 *   <li>Verify it is a fresh inbound message from the customer (not an
 *       outgoing message we just posted ourselves)</li>
 *   <li>Fetch the conversation history from Chatwoot</li>
 *   <li>Call Claude with the persona system prompt + history</li>
 *   <li>Post Claude's reply back into the conversation — as a <b>private note</b>
 *       (human-in-the-loop default) or as a <b>public outgoing message</b>
 *       depending on {@code aiAgent.replyPublic}</li>
 * </ol>
 *
 * <p>Runs asynchronously so the webhook controller can ack Chatwoot in &lt;1s
 * and avoid retries.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAgentService {

    /** Marker line so private-note replies are clearly identifiable as AI. */
    private static final String AI_NOTE_PREFIX = "🤖 AI suggested reply (review before sending):\n\n";

    private final ChatwootProperties props;
    private final ChatwootClient chatwoot;
    private final LlmRouter llm;
    private final ServiceEventLogger eventLogger;

    /**
     * Handle a {@code message_created} webhook payload. Called from the
     * webhook controller; runs on a separate thread so we don't block the
     * Chatwoot HTTP ack.
     *
     * <p>Errors are caught and logged — we never raise; the customer-facing
     * conversation must remain functional even if the AI side breaks.</p>
     */
    @Async
    public void onMessageCreated(JsonNode payload) {
        if (!isEnabled()) {
            log.debug("[AiAgent] disabled — skipping");
            return;
        }
        try {
            // Defense in depth: ignore anything we generated ourselves.
            // message_type=0 (incoming) is from the customer; message_type=1 is from an agent (could be us).
            int messageType = payload.path("message_type").asInt(0);
            boolean privateNote = payload.path("private").asBoolean(false);
            if (messageType != 0 || privateNote) {
                log.debug("[AiAgent] skipping message_type={} private={}", messageType, privateNote);
                return;
            }

            Long conversationId = payload.path("conversation").path("id").asLong();
            if (conversationId == 0L) {
                log.warn("[AiAgent] missing conversation.id in payload — skipping");
                return;
            }

            // Fetch conversation history so Claude has context (not just the
            // single last message). Chatwoot returns messages newest-first;
            // we reverse for Claude (which wants oldest-first).
            List<ChatwootClient.ChatMessage> messages = chatwoot.listMessages(conversationId);
            List<LlmClient.Turn> turns = toLlmTurns(messages);
            if (turns.isEmpty()) {
                log.debug("[AiAgent] no usable history for conversation {} — skipping", conversationId);
                return;
            }

            String reply = llm.complete(turns);
            if (reply == null || reply.isBlank()) {
                log.warn("[AiAgent] empty LLM response — skipping post-back");
                eventLogger.failed("ai.reply", "outbound", null, null,
                        Map.of("conversationId", conversationId, "provider", llm.current().provider()),
                        "llm_empty");
                return;
            }

            if (props.getAiAgent().isReplyPublic()) {
                chatwoot.sendOutboundMessage(conversationId, reply);
            } else {
                chatwoot.sendPrivateNote(conversationId, AI_NOTE_PREFIX + reply);
            }

            eventLogger.ok("ai.reply", "outbound", null, null,
                    Map.of(
                            "conversationId", conversationId,
                            "provider", llm.current().provider(),
                            "model", props.getAiAgent().getModel(),
                            "public", props.getAiAgent().isReplyPublic(),
                            "preview", preview(reply)
                    ));
            log.info("[AiAgent] conversation={} provider={} replied (public={}, {} chars)",
                    conversationId, llm.current().provider(),
                    props.getAiAgent().isReplyPublic(), reply.length());

        } catch (Exception e) {
            log.error("[AiAgent] handling failed: {}", e.getMessage(), e);
            eventLogger.failed("ai.reply", "outbound", null, null,
                    payload == null ? null : payload.toString(), e.getMessage());
        }
    }

    /**
     * Convert Chatwoot's message list to a provider-neutral turn list, mapping
     * incoming → user and outgoing → assistant. Strips private notes and
     * empty messages so the LLM doesn't get confused.
     */
    private List<LlmClient.Turn> toLlmTurns(List<ChatwootClient.ChatMessage> messages) {
        // Chatwoot returns newest-first; flip to oldest-first for the LLM.
        List<ChatwootClient.ChatMessage> ordered = new ArrayList<>(messages);
        java.util.Collections.reverse(ordered);

        List<LlmClient.Turn> turns = new ArrayList<>(ordered.size());
        for (ChatwootClient.ChatMessage m : ordered) {
            if (m.privateNote) continue;
            if (m.content == null || m.content.isBlank()) continue;
            // Skip our own AI private-note prefix if it slipped through somehow.
            if (m.content.startsWith(AI_NOTE_PREFIX.trim())) continue;
            turns.add(m.fromCustomer
                    ? LlmClient.Turn.user(m.content)
                    : LlmClient.Turn.assistant(m.content));
        }
        // Both Anthropic and OpenAI require the first non-system turn to be
        // 'user'. If history starts with an assistant message (rare, but happens
        // for system-greeted conversations), drop the leading assistant turns.
        while (!turns.isEmpty() && !"user".equals(turns.get(0).role())) {
            turns.remove(0);
        }
        return turns;
    }

    private boolean isEnabled() {
        ChatwootProperties.AiAgent ai = props.getAiAgent();
        if (!ai.isEnabled()) return false;
        String provider = ai.getProvider() == null ? "" : ai.getProvider().toLowerCase();
        return switch (provider) {
            case "claude" -> ai.getAnthropicApiKey() != null && !ai.getAnthropicApiKey().isBlank();
            case "openai" -> ai.getOpenaiApiKey() != null && !ai.getOpenaiApiKey().isBlank();
            default -> false;
        };
    }

    private static String preview(String s) {
        if (s == null) return "";
        String oneLine = s.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= 120 ? oneLine : oneLine.substring(0, 117) + "...";
    }
}
