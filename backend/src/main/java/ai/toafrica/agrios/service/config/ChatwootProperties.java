package ai.toafrica.agrios.service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the AgriOS &lt;-&gt; Chatwoot bridge.
 *
 * <p>Sprint 40 v0.1: keep the surface tiny. We need just enough to reach the
 * Chatwoot REST API as an admin agent.</p>
 *
 * <ul>
 *   <li>{@code baseUrl}    - http://chatwoot-web:3000 in docker network,
 *                            http://localhost:3000 from a developer's host</li>
 *   <li>{@code apiToken}   - the user/agent access token shown in Chatwoot
 *                            Profile Settings &gt; Access Token</li>
 *   <li>{@code accountId}  - the Chatwoot account id; almost always 1 unless
 *                            the operator created multiple accounts</li>
 *   <li>{@code enabled}    - when false (default if apiToken is blank), every
 *                            sync call short-circuits with a log line and the
 *                            rest of AgriOS keeps working. Useful in dev
 *                            before the operator has registered Chatwoot.</li>
 * </ul>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "agrios.chatwoot")
public class ChatwootProperties {

    /** http://chatwoot-web:3000 (docker network) or http://localhost:3000 (host). No trailing slash. */
    private String baseUrl = "http://chatwoot-web:3000";

    /** Chatwoot user access token (header {@code api_access_token}). Leave blank to disable. */
    private String apiToken = "";

    /** Chatwoot account id. Default 1. */
    private Long accountId = 1L;

    /** Connect / read timeout in ms. Chatwoot can be slow on first request after idle. */
    private int timeoutMs = 10_000;

    /**
     * Shared secret used to HMAC-sign inbound webhooks. When blank, signature
     * verification is skipped — fine for dev on localhost, NOT fine for prod.
     * Configure via env var {@code CHATWOOT_WEBHOOK_SECRET} (see application.yml)
     * and paste the same string into Chatwoot's webhook configuration UI.
     */
    private String webhookSecret = "";

    /**
     * Sprint 40f: AI Agent settings. When {@link #aiAgent}.enabled is true AND
     * the Anthropic API key is set, incoming customer messages get an automatic
     * draft reply via Claude. Leave disabled to ship a human-only inbox first.
     */
    private AiAgent aiAgent = new AiAgent();

    @lombok.Data
    public static class AiAgent {
        /** Hard switch — false means no AI calls at all. */
        private boolean enabled = false;

        /**
         * Which LLM provider to use. Supported values: {@code "claude"} or
         * {@code "openai"}. Both expose the same chat interface to the rest
         * of the codebase via {@link ai.toafrica.agrios.service.client.LlmRouter}.
         */
        private String provider = "claude";

        // ----- Claude (Anthropic) settings -----

        /** Anthropic API key (sk-ant-...). Required when provider=claude. */
        private String anthropicApiKey = "";

        // ----- OpenAI settings -----

        /** OpenAI API key (sk-...). Required when provider=openai. */
        private String openaiApiKey = "";

        /**
         * Optional override of the OpenAI base URL — set this to talk to Azure
         * OpenAI, Groq, OpenRouter, Together, Ollama, vLLM, or any other
         * OpenAI-compatible gateway. Leave blank for the official OpenAI API.
         */
        private String openaiBaseUrl = "";

        /** Optional OpenAI organization id (for accounts with multiple orgs). */
        private String openaiOrganization = "";

        // ----- Shared generation settings -----

        /**
         * Model identifier. Interpretation depends on {@link #provider}:
         * <ul>
         *   <li>claude: {@code claude-haiku-4-5}, {@code claude-sonnet-4-6}, ...</li>
         *   <li>openai: {@code gpt-4o-mini}, {@code gpt-4o}, ...</li>
         * </ul>
         */
        private String model = "claude-haiku-4-5";

        /** Max tokens in the AI reply. ~300-400 keeps replies short. */
        private int maxTokens = 400;

        /** Temperature 0..1. Lower = more deterministic. 0.4 is a good CSR default. */
        private double temperature = 0.4;

        /**
         * System prompt — defines the agent persona. Operators can override per
         * deployment. Keep it short; verbose system prompts waste tokens.
         */
        private String systemPrompt =
            "You are a friendly customer service assistant for Albert's Farm, a Kenyan farm producing " +
            "fresh fruits and vegetables. Reply concisely in the same language the customer used (English, Swahili, " +
            "or Chinese). If the customer asks about order status, product availability, prices, or anything " +
            "specific you cannot verify from the conversation alone, politely say you will check with a " +
            "human team member and ask them to wait. Never invent prices, quantities, dates, or order numbers.";

        /**
         * If true, the AI reply is sent as a public outbound message visible to
         * the customer. If false, it is sent as a private note — agents see the
         * AI suggestion and can edit before sending. v0.1 default = private
         * (human in the loop).
         */
        private boolean replyPublic = false;
    }

    /** Master switch. Auto-derived from {@link #apiToken} being non-blank. */
    public boolean isEnabled() {
        return apiToken != null && !apiToken.isBlank();
    }

    /** Strip a trailing slash if someone configured one. */
    public String getBaseUrl() {
        if (baseUrl == null) return "";
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
