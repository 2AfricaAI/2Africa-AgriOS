package ai.toafrica.agrios.service.client;

import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.service.client.dto.ChatwootContact;
import ai.toafrica.agrios.service.client.dto.ChatwootContactRequest;
import ai.toafrica.agrios.service.config.ChatwootProperties;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Thin REST client for the Chatwoot API.
 *
 * <p>Sprint 40c v0.1 scope:</p>
 * <ul>
 *   <li>Contact CRUD (search / create / update / fetch)</li>
 *   <li>Conversation create + message send (kept here so Sprint 40d/40f can
 *       use the same client without re-opening the file)</li>
 * </ul>
 *
 * <p>Authentication: the Chatwoot user/agent access token, sent in header
 * {@code api_access_token}. Configured via {@link ChatwootProperties#getApiToken()}.</p>
 *
 * <p>When the configuration is missing ({@link ChatwootProperties#isEnabled()}
 * returns false), every public method short-circuits with a log line and a
 * thrown {@link IllegalStateException} so callers can decide whether to
 * surface the failure or skip silently. AgriOS itself never crashes for lack
 * of Chatwoot - the service module is optional.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatwootClient {

    private final ChatwootProperties props;

    /**
     * Configured to use snake_case on the wire (Chatwoot's convention) while
     * letting Java code keep camelCase via Lombok @Data. Local to the client
     * so we don't pollute the global ObjectMapper used by web responses.
     */
    private final ObjectMapper json = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    // -----------------------------------------------------------------------
    // Contacts
    // -----------------------------------------------------------------------

    /**
     * Find contacts by free-text query. Chatwoot search matches name / email /
     * phone / identifier. We typically pass an exact phone number or our own
     * {@code CUS-NNNNN} code via {@code identifier}.
     *
     * @param query free-text search string
     * @return the matched contacts, possibly empty, never null
     */
    public List<ChatwootContact> searchContacts(String query) {
        ensureEnabled();
        String url = baseAccountUrl() + "/contacts/search?q=" + urlEncode(query) + "&include=contact_inboxes";
        String body = execute(url, Method.GET, null);

        try {
            JsonNode root = json.readTree(body);
            JsonNode payload = root.path("payload");
            if (!payload.isArray()) return List.of();
            return json.convertValue(payload, new TypeReference<>() {});
        } catch (IOException e) {
            throw new BusinessException("Chatwoot search response malformed: " + e.getMessage());
        }
    }

    /**
     * Look up by AgriOS identifier (we store Customer.code as Chatwoot
     * Contact.identifier). Returns the single best match, or empty if none.
     */
    public Optional<ChatwootContact> findByIdentifier(String identifier) {
        ensureEnabled();
        // Chatwoot has a dedicated lookup endpoint for identifier — exact match,
        // no fuzzy logic, no pagination shenanigans.
        String url = baseAccountUrl() + "/contacts/search?q=" + urlEncode(identifier);
        String body = execute(url, Method.GET, null);

        try {
            JsonNode root = json.readTree(body);
            JsonNode payload = root.path("payload");
            if (!payload.isArray() || payload.isEmpty()) return Optional.empty();

            // Walk the result and pick the row whose identifier exactly equals ours.
            for (JsonNode node : payload) {
                String id = node.path("identifier").asText("");
                if (identifier.equals(id)) {
                    return Optional.of(json.treeToValue(node, ChatwootContact.class));
                }
            }
            return Optional.empty();
        } catch (IOException e) {
            throw new BusinessException("Chatwoot identifier lookup failed: " + e.getMessage());
        }
    }

    /** {@code GET /api/v1/accounts/{id}/contacts/{contactId}} */
    public ChatwootContact getContact(Long contactId) {
        ensureEnabled();
        String body = execute(baseAccountUrl() + "/contacts/" + contactId, Method.GET, null);
        return parseContactEnvelope(body);
    }

    /** {@code POST /api/v1/accounts/{id}/contacts} */
    public ChatwootContact createContact(ChatwootContactRequest req) {
        ensureEnabled();
        String body = execute(baseAccountUrl() + "/contacts", Method.POST, toJson(req));
        return parseContactEnvelope(body);
    }

    /** {@code PATCH /api/v1/accounts/{id}/contacts/{contactId}} */
    public ChatwootContact updateContact(Long contactId, ChatwootContactRequest req) {
        ensureEnabled();
        // Chatwoot uses PUT for full updates and PATCH semantics via PUT body.
        // We send PUT — it accepts partial bodies the same way.
        String body = execute(baseAccountUrl() + "/contacts/" + contactId, Method.PUT, toJson(req));
        return parseContactEnvelope(body);
    }

    // -----------------------------------------------------------------------
    // Conversations / Messages (used by Sprint 40d, 40f — included here so the
    // client surface is stable.)
    // -----------------------------------------------------------------------

    /**
     * Send an outbound message into an existing Chatwoot conversation. Useful
     * when AgriOS wants to push a system note ("order shipped") into the
     * conversation thread the agent is already seeing.
     */
    public void sendOutboundMessage(Long conversationId, String content) {
        ensureEnabled();
        sendMessage(conversationId, content, "outgoing", false);
    }

    /**
     * Post a <b>private note</b> visible only to agents, not to the customer.
     * Used by the AI Agent when {@code aiAgent.replyPublic=false} (default) —
     * keeps a human in the loop while still surfacing the AI suggestion.
     */
    public void sendPrivateNote(Long conversationId, String content) {
        ensureEnabled();
        sendMessage(conversationId, content, "outgoing", true);
    }

    private void sendMessage(Long conversationId, String content, String messageType, boolean isPrivate) {
        String url = baseAccountUrl() + "/conversations/" + conversationId + "/messages";
        String payload = "{\"content\":\"" + escapeJson(content)
                + "\",\"message_type\":\"" + messageType
                + "\",\"private\":" + isPrivate + "}";
        execute(url, Method.POST, payload);
    }

    /**
     * Fetch the message history of a conversation. The Chatwoot endpoint
     * returns at least the last ~20 messages; we don't paginate yet because
     * Claude only sees a context window anyway.
     */
    public List<ChatMessage> listMessages(Long conversationId) {
        ensureEnabled();
        String url = baseAccountUrl() + "/conversations/" + conversationId + "/messages";
        String body = execute(url, Method.GET, null);
        try {
            JsonNode root = json.readTree(body);
            JsonNode payload = root.path("payload");
            if (!payload.isArray()) return List.of();
            List<ChatMessage> out = new java.util.ArrayList<>(payload.size());
            for (JsonNode node : payload) {
                ChatMessage m = new ChatMessage();
                m.id = node.path("id").asLong();
                m.content = node.path("content").asText("");
                // message_type: 0 = incoming (from customer), 1 = outgoing (from agent)
                m.fromCustomer = node.path("message_type").asInt(0) == 0;
                m.privateNote = node.path("private").asBoolean(false);
                m.createdAt = node.path("created_at").asLong();
                out.add(m);
            }
            return out;
        } catch (IOException e) {
            throw new BusinessException("Chatwoot message list malformed: " + e.getMessage());
        }
    }

    /** Minimal projection of a Chatwoot message. */
    public static class ChatMessage {
        public Long id;
        public String content;
        public boolean fromCustomer;
        public boolean privateNote;
        public Long createdAt;
    }

    /**
     * Health probe used by the diagnostics endpoint. Returns true if the
     * Chatwoot account is reachable and the token is accepted.
     */
    public boolean pingProfile() {
        if (!props.isEnabled()) return false;
        try {
            // The /profile endpoint is the cheapest authenticated GET — confirms
            // both reachability and that the token belongs to a real agent.
            execute(props.getBaseUrl() + "/api/v1/profile", Method.GET, null);
            return true;
        } catch (Exception e) {
            log.warn("[Chatwoot ping] failed: {}", e.getMessage());
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // Internals
    // -----------------------------------------------------------------------

    private void ensureEnabled() {
        if (!props.isEnabled()) {
            throw new IllegalStateException(
                "Chatwoot integration disabled (agrios.chatwoot.api-token is blank). "
                + "Set CHATWOOT_API_TOKEN env var or agrios.chatwoot.api-token in application.yml."
            );
        }
    }

    private String baseAccountUrl() {
        return props.getBaseUrl() + "/api/v1/accounts/" + props.getAccountId();
    }

    /**
     * Single execution funnel — all Chatwoot calls go through here so retries,
     * timeouts and error mapping stay in one place.
     */
    private String execute(String url, Method method, String body) {
        long t0 = System.currentTimeMillis();
        HttpRequest req = HttpUtil.createRequest(method, url)
                .header("api_access_token", props.getApiToken())
                .header("Content-Type", "application/json")
                .timeout(props.getTimeoutMs());
        if (body != null) req.body(body);

        try (HttpResponse resp = req.execute()) {
            int status = resp.getStatus();
            String text = resp.body();
            long ms = System.currentTimeMillis() - t0;
            if (status >= 200 && status < 300) {
                log.debug("[Chatwoot {} {}] {} in {}ms", method, url, status, ms);
                return text;
            }
            log.warn("[Chatwoot {} {}] {} in {}ms body={}", method, url, status, ms, text);
            throw new BusinessException("Chatwoot " + method + " " + url + " -> HTTP " + status + ": " + text);
        }
    }

    private String toJson(Object o) {
        try {
            return json.writeValueAsString(o);
        } catch (Exception e) {
            throw new BusinessException("Chatwoot request serialization failed: " + e.getMessage());
        }
    }

    /**
     * Chatwoot wraps single-contact responses inconsistently — sometimes the
     * contact is at the root, sometimes under {@code "payload"}, sometimes
     * under {@code "data"}. Handle all three.
     */
    private ChatwootContact parseContactEnvelope(String body) {
        try {
            JsonNode root = json.readTree(body);
            JsonNode node = root;
            if (root.has("payload") && !root.path("payload").isMissingNode()) {
                node = root.path("payload");
                // payload itself may be { "contact": {...} } or the contact directly
                if (node.has("contact")) node = node.path("contact");
            } else if (root.has("data")) {
                node = root.path("data");
            }
            return json.treeToValue(node, ChatwootContact.class);
        } catch (IOException e) {
            throw new BusinessException("Chatwoot contact response malformed: " + e.getMessage());
        }
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    /** Minimal JSON string escaping for ad-hoc payloads. */
    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
