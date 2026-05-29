package ai.toafrica.agrios.framework.importer;

import org.apache.poi.ss.usermodel.Row;

import java.util.List;

/**
 * Sprint 38: contract every Excel-importable entity implements.
 *
 *   - entityKey():   short identifier ("crops") used in i18n keys + filenames.
 *   - headers():     column order in template.xlsx AND expected file order.
 *   - sampleRow():   one example row written under the headers; teaches users
 *                    what to type.
 *   - parseRow():    turn a POI Row into a populated entity. The implementor
 *                    raises ImportException(message, field) for any cell-level
 *                    problem.  The framework already counted the row number.
 *   - persistBatch(): bulk-insert the validated rows in a single transaction.
 *                     Should de-dupe on the unique key, returning how many
 *                     were actually inserted (the rest counted as 'skipped').
 *
 * Returning a count from persistBatch lets the framework report
 * "success vs skipped" without each template having to.
 */
public interface ImportTemplate<T> {
    String entityKey();
    String[] headers();
    String[] sampleRow();

    /**
     * Convert one Excel row into an entity, raising ImportException on any
     * cell-level problem.
     */
    T parseRow(Row row) throws ImportException;

    /**
     * Bulk insert + return how many were actually new (skipped duplicates not
     * counted).
     */
    PersistOutcome persistBatch(List<T> rows);

    record PersistOutcome(int inserted, int skipped) {}
}
