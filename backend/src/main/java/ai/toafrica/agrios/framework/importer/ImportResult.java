package ai.toafrica.agrios.framework.importer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Sprint 38: result envelope returned by every Excel-import endpoint.
 *
 * Behavioural contract:
 *   - All-or-nothing semantics. If any row fails validation, nothing is
 *     persisted and {@code errors} is populated.
 *   - Existing rows (by unique key) are SKIPPED, not failed, and counted
 *     into {@code skipped}.
 *
 * Typical UI flow:
 *   parsed=120, success=98, skipped=20, errors.size()=2
 *     -> show "Imported 98, skipped 20 existing, 2 errors" + per-row error table
 */
@Data
@Schema(description = "Excel import result envelope")
public class ImportResult {
    /** Total data rows read from the file (header row not counted). */
    private int parsed;
    /** Successfully inserted. */
    private int success;
    /** Skipped because the unique key already existed. */
    private int skipped;
    /** Per-row validation errors. Empty if everything passed. */
    private List<ImportRowError> errors = new ArrayList<>();

    public boolean hasErrors() { return !errors.isEmpty(); }

    public void addError(int row, String field, String message, String rawRow) {
        errors.add(new ImportRowError(row, field, message, rawRow));
    }
}
