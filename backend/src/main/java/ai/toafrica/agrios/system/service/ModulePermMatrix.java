package ai.toafrica.agrios.system.service;

import java.util.List;
import java.util.Map;

/**
 * Sprint 36: Static "module x access-level" matrix.
 *
 * The 11 business modules each have a hard-coded list of menu codes that the
 * level translates to.  Level semantics:
 *   - NONE  : zero menus bound.
 *   - READ  : list/view/trace/report menus only (no add/edit/delete buttons).
 *   - WRITE : everything READ has + every action button under those menus.
 *
 * This mapping is intentionally code (not DB rows) because:
 *   1. New menus added by a sprint should automatically flow into the matrix
 *      via the developer updating this file in the same PR.
 *   2. We never want a SUPER_ADMIN to redefine what 'read' means.
 *
 * The matrix's source of truth = the menu codes inserted by migration 039.
 */
public final class ModulePermMatrix {

    private ModulePermMatrix() {}

    public enum Level { NONE, READ, WRITE }

    /** Tier-1 dir codes (home + 10 modules + system). Used by UI as the row list. */
    public static final List<String> MODULES = List.of(
            "home", "master", "production", "warehouse",
            "qc", "packhouse", "sales", "operations",
            "finance", "procurement", "system"
    );

    /**
     * For each module, list the LIST/VIEW menu codes ("read tier").
     * These are the things a read-only user can open from the side menu.
     */
    private static final Map<String, List<String>> READ_MENUS = Map.ofEntries(
        Map.entry("home", List.of("home")),
        Map.entry("master", List.of(
            "master.crops","master.varieties","master.pkgspec",
            "master.warehouses","master.inputs","master.inputStock","master.stockLog")),
        Map.entry("production", List.of(
            "production.plots","production.plans","production.activities",
            "production.harvests","production.batches")),
        Map.entry("warehouse", List.of(
            "warehouse.inbound","warehouse.outbound","warehouse.stocktake",
            "warehouse.transfer","warehouse.scrap","warehouse.reports")),
        Map.entry("qc", List.of(
            "qc.inspections","qc.trace","qc.complaints","qc.recalls","qc.gapReports")),
        Map.entry("packhouse", List.of("packhouse.packings","packhouse.inventory")),
        Map.entry("sales", List.of("sales.customers","sales.orders")),
        Map.entry("operations", List.of("operations.actionBoard")),
        Map.entry("finance", List.of(
            "finance.reports","finance.ar","finance.cashFlow","finance.monthly")),
        Map.entry("procurement", List.of(
            "procurement.suppliers","procurement.orders","procurement.ap")),
        Map.entry("system", List.of("system.users","system.roles"))
    );

    /**
     * For each module, list the action-button codes ("write tier" — added on
     * top of READ).  These are the .add / .edit / .delete / .confirm buttons
     * that gate write operations.
     */
    private static final Map<String, List<String>> WRITE_BUTTON_CODE_PREFIXES = Map.ofEntries(
        Map.entry("home", List.of()),
        Map.entry("master", List.of(
            "master.crops.","master.varieties.","master.pkgspec.",
            "master.warehouses.","master.inputs.")),
        Map.entry("production", List.of(
            "production.plots.","production.plans.","production.activities.",
            "production.harvests.","production.batches.")),
        Map.entry("warehouse", List.of(
            "warehouse.inbound.","warehouse.outbound.","warehouse.stocktake.",
            "warehouse.transfer.","warehouse.scrap.")),
        Map.entry("qc", List.of(
            "qc.inspections.","qc.complaints.","qc.recalls.","qc.gapReports.")),
        Map.entry("packhouse", List.of("packhouse.packings.")),
        Map.entry("sales", List.of("sales.customers.","sales.orders.")),
        Map.entry("operations", List.of("operations.actionBoard.")),
        Map.entry("finance", List.of("finance.reports.","finance.ar.")),
        Map.entry("procurement", List.of(
            "procurement.suppliers.","procurement.orders.","procurement.ap.")),
        Map.entry("system", List.of(
            "system.users.","system.roles."))
    );

    public static List<String> readMenuCodes(String module) {
        return READ_MENUS.getOrDefault(module, List.of());
    }

    /**
     * Returns code prefixes for all action buttons gated by 'write' on this
     * module.  Caller does:  WHERE m.code LIKE prefix||'%'
     */
    public static List<String> writeButtonPrefixes(String module) {
        return WRITE_BUTTON_CODE_PREFIXES.getOrDefault(module, List.of());
    }
}
