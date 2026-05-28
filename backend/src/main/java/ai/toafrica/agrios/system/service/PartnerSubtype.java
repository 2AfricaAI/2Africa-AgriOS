package ai.toafrica.agrios.system.service;

import java.util.List;

/**
 * Sprint 37: the canonical list of partner subtype codes.
 * These are not an enum so SUPER_ADMIN can add new subtypes via the DB without
 * a code change, but the well-known ones live here for the form drop-downs.
 */
public final class PartnerSubtype {
    private PartnerSubtype() {}

    public static final String AGRONOMIST   = "AGRONOMIST";
    public static final String GAP_AUDITOR  = "GAP_AUDITOR";
    public static final String BANK_OFFICER = "BANK_OFFICER";
    public static final String LANDLORD     = "LANDLORD";
    public static final String INSURANCE    = "INSURANCE";

    public static final List<String> ALL = List.of(
            AGRONOMIST, GAP_AUDITOR, BANK_OFFICER, LANDLORD, INSURANCE
    );

    /** Default role code each subtype maps to when creating the account. */
    public static String defaultRoleCodeFor(String subtype) {
        // 1:1 today; could go richer later.
        return subtype;
    }
}
