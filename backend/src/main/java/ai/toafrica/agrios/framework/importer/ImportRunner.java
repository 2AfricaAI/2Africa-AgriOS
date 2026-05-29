package ai.toafrica.agrios.framework.importer;

import ai.toafrica.agrios.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Sprint 38: orchestrates the Excel-import pipeline.
 *
 *   run(template, file)
 *     1. Open XLSX
 *     2. Skip the header row
 *     3. For each data row -> template.parseRow() catching ImportException
 *     4. If any errors -> return result without persisting (all-or-nothing)
 *     5. Else template.persistBatch() and return success/skipped counts
 *
 *   buildTemplate(template) writes a blank XLSX with a header row + one
 *   sample row so users have something to fill out.
 */
@Slf4j
@Component
public class ImportRunner {

    public <T> ImportResult run(ImportTemplate<T> template, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File is empty");
        }
        ImportResult result = new ImportResult();
        List<T> entities = new ArrayList<>();
        int headerLen = template.headers().length;

        try (InputStream in = file.getInputStream();
             Workbook wb = new XSSFWorkbook(in)) {
            Sheet sh = wb.getSheetAt(0);
            int last = sh.getLastRowNum();

            for (int i = 1; i <= last; i++) {       // 0 = header
                Row row = sh.getRow(i);
                if (row == null || isBlankRow(row, headerLen)) continue;
                result.setParsed(result.getParsed() + 1);
                int rowNumber = i + 1;              // 1-based for users
                try {
                    T entity = template.parseRow(row);
                    if (entity != null) entities.add(entity);
                } catch (ImportException ex) {
                    result.addError(rowNumber, ex.getField(), ex.getMessage(),
                            XlsxParser.rowToCsv(row, headerLen));
                } catch (Exception ex) {
                    log.warn("[Import] row {} unexpected error: {}", rowNumber, ex.getMessage());
                    result.addError(rowNumber, "_row", ex.getMessage() == null
                            ? ex.getClass().getSimpleName() : ex.getMessage(),
                            XlsxParser.rowToCsv(row, headerLen));
                }
            }
        } catch (IOException e) {
            throw new BusinessException("Failed to read Excel file: " + e.getMessage());
        }

        if (result.hasErrors()) {
            log.info("[Import:{}] {} parsed, {} errors -> nothing persisted",
                    template.entityKey(), result.getParsed(), result.getErrors().size());
            return result;
        }

        ImportTemplate.PersistOutcome out = template.persistBatch(entities);
        result.setSuccess(out.inserted());
        result.setSkipped(out.skipped());
        log.info("[Import:{}] parsed={} success={} skipped={}",
                template.entityKey(), result.getParsed(), result.getSuccess(), result.getSkipped());
        return result;
    }

    /** Generate a blank workbook (header + one sample row) for download. */
    public <T> byte[] buildTemplate(ImportTemplate<T> template) {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet(template.entityKey());

            // Header row (bold)
            Row header = sh.createRow(0);
            var headerStyle = wb.createCellStyle();
            var headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            String[] headers = template.headers();
            for (int i = 0; i < headers.length; i++) {
                var cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Sample row (italic, light grey)
            String[] sample = template.sampleRow();
            if (sample != null && sample.length > 0) {
                Row sampleRow = sh.createRow(1);
                var sampleStyle = wb.createCellStyle();
                var sampleFont = wb.createFont();
                sampleFont.setItalic(true);
                sampleFont.setColor((short) 22);    // greyish
                sampleStyle.setFont(sampleFont);
                for (int i = 0; i < sample.length && i < headers.length; i++) {
                    var cell = sampleRow.createCell(i);
                    cell.setCellValue(sample[i]);
                    cell.setCellStyle(sampleStyle);
                }
            }

            for (int i = 0; i < headers.length; i++) sh.autoSizeColumn(i);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("Failed to build template: " + e.getMessage());
        }
    }

    private static boolean isBlankRow(Row row, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            String s = XlsxParser.str(row, i);
            if (s != null && !s.isBlank()) return false;
        }
        return true;
    }
}
