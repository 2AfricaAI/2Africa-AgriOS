package ai.toafrica.agrios.framework.importer;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Sprint 38: cell-level helpers used by every ImportTemplate to read POI
 * cells defensively (Excel turns numbers into strings and vice versa, and
 * empty cells crash if you don't null-check).
 *
 * All helpers null-tolerant: empty / missing cell -> null (caller decides
 * if that's a validation error).
 */
public final class XlsxParser {
    private XlsxParser() {}

    public static String str(Row row, int col) {
        if (row == null) return null;
        Cell c = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (c == null) return null;
        return switch (c.getCellType()) {
            case STRING -> trimToNull(c.getStringCellValue());
            case NUMERIC -> {
                double d = c.getNumericCellValue();
                if (d == Math.floor(d) && !Double.isInfinite(d)) {
                    yield String.valueOf((long) d);
                }
                yield String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(c.getBooleanCellValue());
            case FORMULA -> {
                try { yield trimToNull(c.getStringCellValue()); }
                catch (Exception e) { yield String.valueOf(c.getNumericCellValue()); }
            }
            default -> null;
        };
    }

    public static Integer intVal(Row row, int col, String field) {
        String s = str(row, col);
        if (s == null) return null;
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) {
            throw new ImportException(field, "must be a whole number, got '" + s + "'");
        }
    }

    public static Long longVal(Row row, int col, String field) {
        String s = str(row, col);
        if (s == null) return null;
        try { return Long.parseLong(s.trim()); }
        catch (NumberFormatException e) {
            throw new ImportException(field, "must be a whole number, got '" + s + "'");
        }
    }

    public static java.math.BigDecimal decVal(Row row, int col, String field) {
        String s = str(row, col);
        if (s == null) return null;
        try { return new java.math.BigDecimal(s.trim()); }
        catch (NumberFormatException e) {
            throw new ImportException(field, "must be a number, got '" + s + "'");
        }
    }

    public static LocalDate dateVal(Row row, int col, String field) {
        if (row == null) return null;
        Cell c = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (c == null) return null;
        try {
            if (c.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(c)) {
                return c.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
            String s = str(row, col);
            if (s == null) return null;
            return LocalDate.parse(s.trim());   // ISO yyyy-MM-dd
        } catch (Exception e) {
            throw new ImportException(field, "must be a date YYYY-MM-DD, got cell value");
        }
    }

    public static String requireStr(Row row, int col, String field) {
        String s = str(row, col);
        if (s == null || s.isBlank()) throw new ImportException(field, "is required");
        return s.trim();
    }

    public static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /** Verbatim CSV row dump, used when reporting an error so the UI can
     *  rebuild an "errors-only" workbook for re-upload. */
    public static String rowToCsv(Row row, int columnCount) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < columnCount; i++) {
            if (i > 0) sb.append(',');
            String v = str(row, i);
            if (v == null) continue;
            if (v.contains(",") || v.contains("\"")) {
                sb.append('"').append(v.replace("\"", "\"\"")).append('"');
            } else {
                sb.append(v);
            }
        }
        return sb.toString();
    }
}
