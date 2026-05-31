package ai.toafrica.agrios.service.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Body for {@code POST /api/v1/accounts/{id}/contacts} and the corresponding
 * PATCH. Empty fields are dropped so a PATCH only sends what we want to change.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatwootContactRequest {
    private String name;
    private String email;
    private String phoneNumber;
    private String identifier;
    private Map<String, Object> customAttributes;
}
