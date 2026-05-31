package ai.toafrica.agrios.service.client;

import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.service.config.ChatwootProperties;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link LlmClient} implementation for OpenAI's Chat Completions API. Also
 * works against Azure OpenAI and any OpenAI-compatible gateway (Groq,
 * Together, OpenRouter, vLLM, Ollama, etc.) by overriding the base URL.
 *
 * <p>API doc: https://platform.openai.com/docs/api-reference/chat</p>
 *
 * <p>Used when {@code agrios.chatwoot.ai-agent.provider=openai}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiClient implements LlmClient {

    /** Default endpoint; operators can override for Azure / proxies. */
    private static final String DEFAULT_BASE_URL = "https://api.openai.com";
    private static final String CHAT_PATH = "/v1/chat/completions";

    private final ChatwootProperties props;
    private final ObjectMapper json = new ObjectMapper();

    @Override
    public String provider() {
        return "openai";
    }

    @Override
    public String complete(List<Turn> turns) {
        ChatwootProperties.AiAgent ai = props.getAiAgent();
        if (ai.getOpenaiApiKey() == null || ai.getOpenaiApiKey().isBlank()) {
            throw new IllegalStateException("OpenAI API key is not configured (set OPENAI_API_KEY)");
        }

        // OpenAI's chat API takes the system prompt as a first message of role
        // 'system' (Anthropic separates it). Both shapes are user-facing
        // equivalent for the same prompt.
        List<Map<String, String>> messages = new ArrayList<>(turns.size() + 1);
        if (ai.getSystemPrompt() != null && !ai.getSystemPrompt().isBlank()) {
            messages.add(Map.of("role", "system", "content", ai.getSystemPrompt()));
        }
        for (Turn t : turns) {
            messages.add(Map.of("role", t.role(), "content", t.content()));
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", ai.getModel());
        body.put("max_tokens", ai.getMaxTokens());
        body.put("temperature", ai.getTemperature());
        body.put("messages", messages);

        String url = resolveBaseUrl(ai) + CHAT_PATH;
        long t0 = System.currentTimeMillis();

        HttpRequest req = HttpUtil.createRequest(Method.POST, url)
                .header("Authorization", "Bearer " + ai.getOpenaiApiKey())
                .header("Content-Type", "application/json")
                .body(toJson(body))
                .timeout(30_000);

        // OpenAI for organizations supports an extra header — pass-through if set.
        if (ai.getOpenaiOrganization() != null && !ai.getOpenaiOrganization().isBlank()) {
            req.header("OpenAI-Organization", ai.getOpenaiOrganization(), true);
        }

        try (HttpResponse resp = req.execute()) {
            long ms = System.currentTimeMillis() - t0;
            int status = resp.getStatus();
            String text = resp.body();
            if (status < 200 || status >= 300) {
                log.warn("[OpenAI] HTTP {} in {}ms — body={}", status, ms, text);
                throw new BusinessException("OpenAI API HTTP " + status + ": " + truncate(text, 300));
            }
            log.debug("[OpenAI] ok in {}ms", ms);
            return extractText(text);
        }
    }

    /**
     * OpenAI response: {@code { "choices": [ { "message": { "content": "..." } } ] } }.
     * We take the first choice. Returns empty string on a malformed response —
     * caller decides whether that is bad enough to surface.
     */
    private String extractText(String body) {
        try {
            JsonNode root = json.readTree(body);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) return "";
            return choices.get(0).path("message").path("content").asText("").trim();
        } catch (Exception e) {
            log.warn("[OpenAI] response parse failed: {}", e.getMessage());
            return "";
        }
    }

    private String resolveBaseUrl(ChatwootProperties.AiAgent ai) {
        String base = ai.getOpenaiBaseUrl();
        if (base == null || base.isBlank()) return DEFAULT_BASE_URL;
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    private String toJson(Object o) {
        try {
            return json.writeValueAsString(o);
        } catch (Exception e) {
            throw new BusinessException("OpenAI request serialization failed: " + e.getMessage());
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
