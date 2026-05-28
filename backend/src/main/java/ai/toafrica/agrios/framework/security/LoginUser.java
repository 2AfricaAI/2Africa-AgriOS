package ai.toafrica.agrios.framework.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Login context user info - stored in SecurityContext.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser {
    private Long userId;
    private String username;
    private String dataScope;        // self/group/all
    private Set<String> permissions; // perms strings, e.g. plot:list
    private Set<String> roleCodes;   // role codes

    /** Sprint 37: STAFF / PARTNER / CUSTOMER. */
    private String userType;
    /** Sprint 37: for CUSTOMER user_type, the customer.id this account represents. */
    private Long linkedCustomerId;
}
