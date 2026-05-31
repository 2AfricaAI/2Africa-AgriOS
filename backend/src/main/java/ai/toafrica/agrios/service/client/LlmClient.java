package ai.toafrica.agrios.service.client;

import java.util.List;

/**
 * Provider-agnostic chat-completion interface.
 *
 * <p>Sprint 40f v0.1 supports two implementations: {@link ClaudeClient}
 * (Anthropic) and {@link OpenAiClient} (OpenAI / Azure / OpenAI-compatible
 * gateways). The {@link LlmRouter} picks the right one at runtime based on
 * {@code agrios.chatwoot.ai-agent.provider}.</p>
 *
 * <p>We deliberately keep the interface tiny — adding tools, streaming, or
 * vision payloads will happen when a real product need shows up, not before.</p>
 */
public interface LlmClient {

    /** Stable identifier matched against config. */
    String provider();

    /** Return the assistant's text completion. Throws on transport / auth errors. */
    String complete(List<Turn> turns);

    /**
     * One message in the conversation. {@code role} is always {@code "user"}
     * or {@code "assistant"} — both providers normalize to this shape.
     */
    record Turn(String role, String content) {
        public static Turn user(String content) { return new Turn("user", content); }
        public static Turn assistant(String content) { return new Turn("assistant", content); }
    }
}
