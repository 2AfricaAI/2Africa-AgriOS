package ai.toafrica.agrios.framework.importer;

import lombok.Getter;

/**
 * Sprint 38: per-cell error raised inside ImportTemplate.parseRow.
 * The framework adds the row number from the iterator's position.
 */
@Getter
public class ImportException extends RuntimeException {
    /** Column name as it appears in headers(), e.g. "shelf_life_days". */
    private final String field;

    public ImportException(String field, String message) {
        super(message);
        this.field = field;
    }
}
