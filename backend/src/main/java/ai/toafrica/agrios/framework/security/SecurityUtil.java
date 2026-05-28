package ai.toafrica.agrios.framework.security;

import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.common.R;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 获取当前登录用户的便捷工具
 */
public final class SecurityUtil {
    private SecurityUtil() {}

    public static LoginUser current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof LoginUser u)) {
            throw new BusinessException(R.UNAUTHORIZED, "Not logged in");
        }
        return u;
    }

    public static Long currentUserId() {
        return current().getUserId();
    }

    public static boolean hasPerm(String perm) {
        try {
            return current().getPermissions().contains(perm);
        } catch (Exception e) {
            return false;
        }
    }

    /** Sprint 37: STAFF / PARTNER / CUSTOMER (defaults to STAFF if not in JWT). */
    public static String currentUserType() {
        try {
            String t = current().getUserType();
            return t == null ? "STAFF" : t;
        } catch (Exception e) {
            return "STAFF";
        }
    }

    /** True if the current request is from a CUSTOMER self-service account. */
    public static boolean isCurrentCustomer() {
        return "CUSTOMER".equals(currentUserType());
    }

    /** For CUSTOMER user_type, the customer.id this account is bound to. */
    public static Long currentLinkedCustomerId() {
        try {
            return current().getLinkedCustomerId();
        } catch (Exception e) {
            return null;
        }
    }
}
