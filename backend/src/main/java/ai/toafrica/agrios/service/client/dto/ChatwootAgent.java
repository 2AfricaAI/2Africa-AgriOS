package ai.toafrica.agrios.service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Slim projection of a Chatwoot Agent (user) for the assignee picker in
 * the AgriOS-native Customer Service UI.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatwootAgent {
    private Long id;
    private String name;
    private String email;
    /** administrator / agent — controls what UI we surface. */
    private String role;
    /** online / offline / busy */
    private String availabilityStatus;
    private String thumbnail;
    private String confirmed;
}
