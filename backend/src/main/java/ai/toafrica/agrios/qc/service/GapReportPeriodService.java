package ai.toafrica.agrios.qc.service;

import ai.toafrica.agrios.production.entity.Batch;
import ai.toafrica.agrios.production.mapper.BatchMapper;
import ai.toafrica.agrios.qc.vo.GapReportData;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Period-level GAP / HACCP report (Sprint 28).
 *
 * Generates a single Excel workbook with one row per batch in a given
 * harvest-date range (optionally filtered by crop). Each row carries the
 * compliance verdict plus quick links to dig into the per-batch detail
 * report. Useful for monthly / quarterly export-audit packs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GapReportPeriodService {

    private final BatchMapper batchMapper;
    private final GapReportService gapReportService;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public byte[] render(LocalDate from, LocalDate to, Long cropId) {
        List<Batch> batches = batchMapper.selectList(
                new LambdaQueryWrapper<Batch>()
                        .ge(from != null, Batch::getHarvestDate, from)
                        .le(to   != null, Batch::getHarvestDate, to)
                        .eq(cropId != null, Batch::getCropId, cropId)
                        .isNull(Batch::getDeletedAt)
                        .orderByAsc(Batch::getHarvestDate)
                        .orderByAsc(Batch::getId));

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            GapReportXlsxService.Styles s = new GapReportXlsxService.Styles(wb);
            Sheet sh = wb.createSheet("GAP Period Report");

            // Header info
            Row meta = sh.createRow(0);
            cell(meta, 0, "GAP / HACCP Period Report", s.titleStyle);
            Row range = sh.createRow(1);
            cell(range, 0, "From", s.keyStyle);
            cell(range, 1, from == null ? "—" : from.format(DATE_FMT), s.dataStyle);
            cell(range, 2, "To", s.keyStyle);
            cell(range, 3, to == null ? "—" : to.format(DATE_FMT), s.dataStyle);
            cell(range, 4, "Batches", s.keyStyle);
            cell(range, 5, String.valueOf(batches.size()), s.dataStyle);

            // Table header
            String[] cols = {
                    "Batch Code", "Crop", "Variety", "Plot", "Harvest Date", "Qty (kg)",
                    "Verdict", "PHI", "QC Pass", "QC Fail", "Complaints", "Recalls",
                    "Status"
            };
            Row h = sh.createRow(3);
            for (int i = 0; i < cols.length; i++) cell(h, i, cols[i], s.headerStyle);

            int r = 4;
            for (Batch b : batches) {
                GapReportData d;
                try {
                    d = gapReportService.buildBatchReport(b.getCode());
                } catch (Exception ex) {
                    log.warn("Skip batch {} in period report: {}", b.getCode(), ex.getMessage());
                    continue;
                }
                Row row = sh.createRow(r++);
                cell(row, 0, b.getCode(), s.dataStyle);
                cell(row, 1, safe(d.getTrace().getBatch().getCropName()), s.dataStyle);
                cell(row, 2, safe(d.getTrace().getBatch().getVarietyName()), s.dataStyle);
                cell(row, 3, d.getTrace().getPlot() == null ? "" : safe(d.getTrace().getPlot().getCode()), s.dataStyle);
                cell(row, 4, b.getHarvestDate() == null ? "" : b.getHarvestDate().format(DATE_FMT), s.dataStyle);
                cell(row, 5, b.getQtyKg() == null ? "0" : b.getQtyKg().stripTrailingZeros().toPlainString(), s.dataStyle);

                String verdict = d.getVerdict();
                CellStyle vStyle = "COMPLIANT".equals(verdict) ? s.okStyle
                        : "FLAGGED".equals(verdict) ? s.warnStyle
                        : "NON_COMPLIANT".equals(verdict) ? s.failStyle : s.dataStyle;
                cell(row, 6, verdict, vStyle);

                boolean phiOk = d.getPhiCompliance() == null || d.getPhiCompliance().isCompliant();
                cell(row, 7, phiOk ? "OK" : "BREACH", phiOk ? s.okStyle : s.failStyle);

                int pass = d.getQcSummary() == null ? 0 : d.getQcSummary().getPass();
                int fail = d.getQcSummary() == null ? 0 : d.getQcSummary().getFail();
                cell(row, 8, String.valueOf(pass), pass > 0 ? s.okStyle : s.dataStyle);
                cell(row, 9, String.valueOf(fail), fail > 0 ? s.failStyle : s.dataStyle);

                int cCount = d.getComplaints() == null ? 0 : d.getComplaints().size();
                int rCount = d.getRecalls() == null ? 0 : d.getRecalls().size();
                cell(row, 10, String.valueOf(cCount), cCount > 0 ? s.warnStyle : s.dataStyle);
                cell(row, 11, String.valueOf(rCount), rCount > 0 ? s.failStyle : s.dataStyle);

                cell(row, 12, safe(b.getStatus()), s.dataStyle);
            }

            // Column widths
            int[] widths = {18, 14, 14, 14, 14, 12, 16, 10, 10, 10, 12, 10, 12};
            for (int i = 0; i < widths.length; i++) sh.setColumnWidth(i, widths[i] * 256);

            wb.write(os);
            byte[] bytes = os.toByteArray();
            log.info("[GapReportPeriod] from={} to={} cropId={} batches={} xlsx={}KB",
                    from, to, cropId, batches.size(), bytes.length / 1024);
            return bytes;
        } catch (Exception e) {
            throw new RuntimeException("Failed to render GAP period report", e);
        }
    }

    private void cell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value == null ? "" : value);
        if (style != null) c.setCellStyle(style);
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
