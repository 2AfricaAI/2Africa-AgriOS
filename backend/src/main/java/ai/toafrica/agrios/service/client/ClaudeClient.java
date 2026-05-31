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
 * {@link LlmClient} implementation for Anthropic's Claude (Messages API).
 *
 * <p>API doc: https://docs.anthropic.com/en/api/messages</p>
 *
 * <p>Used when {@code agrios.chatwoot.ai-agent.provider=claude}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClaudeClient implements LlmClient {

    private static final String DEFAULT_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String API_VERSION = "2023-06-01";

    private final ChatwootProperties props;
    private final ObjectMapper json = new ObjectMapper();

    @Override
    public String provider() {
        return "claude";
    }

    @Override
    public String complete(List<Turn> turns) {
        ChatwootProperties.AiAgent ai = props.getAiAgent();
        if (ai.getAnthropicApiKey() == null || ai.getAnthropicApiKey().isBlank()) {
            throw new IllegalStateException("Anthropic API key is not configured (set ANTHROPIC_API_KEY)");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", ai.getModel());
        body.put("max_tokens", ai.getMaxTokens());
        body.put("temperature", ai.getTemperature());
        if (ai.getSystemPrompt() != null && !ai.getSystemPrompt().isBlank()) {
            body.put("system", ai.getSystemPrompt());
        }
        List<Map<String, String>> messages = new ArrayList<>();
        for (Turn t : turns) {
            messages.add(Map.of("role", t.role(), "content", t.content()));
        }
        body.put("messages", messages);

        long t0 = System.currentTimeMillis();
        HttpRequest req = HttpUtil.createRequest(Method.POST, DEFAULT_API_URL)
                .header("x-api-key", ai.getAnthropicApiKey())
                .header("anthropic-version", API_VERSION)
                .header("Content-Type", "application/json")
                .body(toJson(body))
                .timeout(30_000);

        try (HttpResponse resp = req.execute()) {
            long ms = System.currentTimeMillis() - t0;
            int status = resp.getStatus();
            String text = resp.body();
            if (status < 200 || status >= 300) {
                log.warn("[Claude] HTTP {} in {}ms — body={}", status, ms, text);
                throw new BusinessException("Claude API HTTP " + status + ": " + truncate(text, 300));
            }
            log.debug("[Claude] ok in {}ms", ms);
            return extractText(text);
        }
    }

    /**
     * Claude's response: {@code { "content": [ { "type": "text", "text": "..." } ] } }.
     * We concatenate all text blocks (usually just one) and trim.
     */
    private String extractText(String body) {
        try {
            JsonNode root = json.readTree(body);
            JsonNode content = root.path("content");
            if (!content.isArray()) return "";
            StringBuilder sb = new StringBuilder();
            for (JsonNode block : content) {
                if ("text".equals(block.path("type").asText())) {
                    sb.append(block.path("text").asText(""));
                }
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.warn("[Claude] response parse failed: {}", e.getMessage());
            return "";
        }
    }

    private String toJson(Object o) {
        try {
            return json.writeValueAsString(o);
        } catch (Exception e) {
            throw new BusinessException("Claude request serialization failed: " + e.getMessage());
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
