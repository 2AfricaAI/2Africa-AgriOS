package ai.toafrica.agrios.service.client;

import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.service.config.ChatwootProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Picks the configured {@link LlmClient} (claude / openai) at call time.
 *
 * <p>We register every {@link LlmClient} implementation in the Spring context
 * and route by name. Adding a new provider is a single class — no need to
 * touch this router or {@link ai.toafrica.agrios.service.service.AiAgentService}.</p>
 */
@Slf4j
@Component
public class LlmRouter {

    private final ChatwootProperties props;
    private final Map<String, LlmClient> byProvider;

    public LlmRouter(ChatwootProperties props, List<LlmClient> clients) {
        this.props = props;
        this.byProvider = clients.stream()
                .collect(Collectors.toMap(
                        LlmClient::provider,
                        Function.identity(),
                        (a, b) -> a   // duplicate-key tiebreaker (shouldn't happen)
                ));
        log.info("[LlmRouter] registered providers: {}", byProvider.keySet());
    }

    /** Returns the configured provider's client. Throws if unknown / missing. */
    public LlmClient current() {
        String key = props.getAiAgent().getProvider();
        if (key == null || key.isBlank()) {
            throw new BusinessException("ai-agent.provider is not set (expected 'claude' or 'openai')");
        }
        LlmClient client = byProvider.get(key.toLowerCase());
        if (client == null) {
            throw new BusinessException("Unknown ai-agent.provider='" + key + "', available=" + byProvider.keySet());
        }
        return client;
    }

    /** Convenience: complete via the currently-configured provider. */
    public String complete(List<LlmClient.Turn> turns) {
        return current().complete(turns);
    }
}
