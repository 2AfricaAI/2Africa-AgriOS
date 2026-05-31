package ai.toafrica.agrios.service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

/**
 * Slim view of a Chatwoot Contact. We deliberately ignore unknown fields so
 * the DTO survives Chatwoot API additions across versions.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatwootContact {
    private Long id;
    private String name;
    private String email;

    /** E.164 phone number. Chatwoot stores it loosely so we never assume format. */
    private String phoneNumber;

    /** Free-form external identifier — we set this to AgriOS Customer.code (e.g. CUS-00042). */
    private String identifier;

    /** Open key/value store on Chatwoot's side — we mirror AgriOS Customer.id here. */
    private Map<String, Object> customAttributes;

    /** Mainly useful for debugging; Chatwoot returns this for created/updated rows. */
    private String createdAt;
}
