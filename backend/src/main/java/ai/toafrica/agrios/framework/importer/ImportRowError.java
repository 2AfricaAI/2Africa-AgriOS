package ai.toafrica.agrios.framework.importer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sprint 38: one row-level failure in an Excel import.
 *
 *  rowNumber  = 1-based row in the user's spreadsheet (matches what they see)
 *  field      = which column had the problem ("code", "shelf_life_days", ...)
 *  message    = human-readable reason ("must be a positive integer")
 *  rawRow     = the raw cell values verbatim, so the UI can render a "errors-only"
 *               download where the user fixes those rows and re-uploads.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "One row-level failure during an Excel import")
public class ImportRowError {
    private int rowNumber;
    private String field;
    private String message;
    private String rawRow;
}
