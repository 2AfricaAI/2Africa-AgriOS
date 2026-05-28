package ai.toafrica.agrios.qc.service;

import ai.toafrica.agrios.qc.vo.GapReportData;
import ai.toafrica.agrios.qc.vo.TraceVO;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * GAP / HACCP audit report — PDF renderer (Sprint 28).
 *
 * One-batch comprehensive PDF intended for GlobalG.A.P / KEPHIS auditors:
 *   Header + verdict badge
 *   Batch + crop/variety + grower info
 *   Plot + planting plan
 *   Field activities (chronological, with PHI checks)
 *   PHI compliance verdict
 *   Inbound (input source / supplier trace)
 *   QC inspections summary
 *   Packing + shipments
 *   Complaints / recalls (if any)
 *   Auditor signature area
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GapReportPdfService {

    private final GapReportService gapReportService;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] render(String batchCode) {
        GapReportData d = gapReportService.buildBatchReport(batchCode);
        String html = buildHtml(d);
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder b = new PdfRendererBuilder();
            b.useFastMode();
            b.withHtmlContent(html, null);
            b.toStream(os);
            b.run();
            byte[] bytes = os.toByteArray();
            log.info("[GapReportPdf] batch={} pdf={}KB verdict={}",
                    batchCode, bytes.length / 1024, d.getVerdict());
            return bytes;
        } catch (Exception e) {
            throw new RuntimeException("Failed to render GAP report PDF", e);
        }
    }

    private String buildHtml(GapReportData d) {
        TraceVO t = d.getTrace();
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html xmlns=\"http://www.w3.org/1999/xhtml\"><head>");
        sb.append("<meta charset=\"UTF-8\"/><title>GAP / HACCP Report — ").append(esc(t.getBatch().getCode())).append("</title>");
        sb.append(CSS);
        sb.append("</head><body>");

        // ---- Header ----
        sb.append("<div class=\"head\">");
        sb.append("<div class=\"brand\"><span class=\"logo\">2A</span> 2Africa AgriOS</div>");
        sb.append("<div class=\"doc-title\">GAP / HACCP AUDIT REPORT</div>");
        sb.append("</div>");

        // ---- Verdict banner ----
        sb.append("<div class=\"verdict ").append(verdictClass(d.getVerdict())).append("\">");
        sb.append("<span class=\"verdict-label\">Overall Verdict</span>");
        sb.append("<span class=\"verdict-value\">").append(esc(d.getVerdict())).append("</span>");
        sb.append("</div>");

        // ---- Batch + meta ----
        sb.append("<table class=\"meta\"><tr>");
        sb.append("<td class=\"meta-l\">");
        sb.append("<div class=\"label\">Batch</div>");
        sb.append("<div class=\"big mono\">").append(esc(t.getBatch().getCode())).append("</div>");
        sb.append("<div class=\"dim\">").append(esc(t.getBatch().getCropName()));
        if (t.getBatch().getVarietyName() != null) sb.append(" &middot; ").append(esc(t.getBatch().getVarietyName()));
        sb.append("</div>");
        sb.append("<div class=\"dim\">").append(qty(t.getBatch().getQtyKg())).append(" kg &middot; ");
        sb.append("Created ").append(fmtDate(t.getBatch().getCreatedDate())).append("</div>");
        sb.append("</td><td class=\"meta-r\">");
        sb.append("<div class=\"label\">Report Generated</div>");
        sb.append("<div class=\"dim\">").append(fmtDateTime(d.getGeneratedAt())).append("</div>");
        if (d.getGeneratedByName() != null) sb.append("<div class=\"dim\">By ").append(esc(d.getGeneratedByName())).append("</div>");
        sb.append("</td></tr></table>");

        // ---- Origin ----
        sb.append("<h3>1. Origin &mdash; Plot &amp; Planting Plan</h3>");
        sb.append("<table class=\"data\"><tbody>");
        if (t.getPlot() != null) {
            sb.append("<tr><td class=\"k\">Plot</td><td class=\"mono\">").append(esc(t.getPlot().getCode())).append("</td>");
            sb.append("<td class=\"k\">Name</td><td>").append(esc(t.getPlot().getName())).append("</td></tr>");
            sb.append("<tr><td class=\"k\">Region</td><td>").append(esc(safe(t.getPlot().getRegionName())));
            sb.append("</td><td class=\"k\">Area (ha)</td><td>").append(qty(t.getPlot().getAreaHa())).append("</td></tr>");
        }
        if (t.getPlan() != null) {
            sb.append("<tr><td class=\"k\">Plan Code</td><td class=\"mono\">").append(esc(t.getPlan().getCode())).append("</td>");
            sb.append("<td class=\"k\">Sow Date</td><td>").append(fmtDate(t.getPlan().getPlannedSowDate())).append("</td></tr>");
            sb.append("<tr><td class=\"k\">Planned Harvest</td><td>").append(fmtDate(t.getPlan().getPlannedHarvestDate())).append("</td>");
            sb.append("<td class=\"k\">Status</td><td>").append(esc(safe(t.getPlan().getStatus()))).append("</td></tr>");
        }
        sb.append("</tbody></table>");

        // ---- Field activities ----
        sb.append("<h3>2. Field Activities &mdash; Chronological Log</h3>");
        if (t.getActivities() == null || t.getActivities().isEmpty()) {
            sb.append("<div class=\"empty\">No field activities recorded.</div>");
        } else {
            sb.append("<table class=\"data\"><thead><tr>");
            sb.append("<th>Date</th><th>Type</th><th>Operator</th><th>Inputs Used</th>");
            sb.append("</tr></thead><tbody>");
            for (TraceVO.ActivityNode a : t.getActivities()) {
                sb.append("<tr>");
                sb.append("<td>").append(fmtDate(a.getOccurDate())).append("</td>");
                sb.append("<td><span class=\"badge badge-").append(actClass(a.getActivityType())).append("\">").append(esc(a.getActivityType())).append("</span></td>");
                sb.append("<td>").append(esc(safe(a.getOperatorName()))).append("</td>");
                sb.append("<td>");
                if (a.getInputs() == null || a.getInputs().isEmpty()) sb.append("<span class=\"dim\">&mdash;</span>");
                else {
                    sb.append("<ul class=\"input-list\">");
                    for (TraceVO.InputUsed iu : a.getInputs()) {
                        sb.append("<li><code>").append(esc(iu.getInputItemCode())).append("</code> ").append(esc(iu.getInputItemName()));
                        if (iu.getActiveIngredient() != null) sb.append(" <span class=\"dim\">(").append(esc(iu.getActiveIngredient())).append(")</span>");
                        sb.append(" &middot; ").append(qty(iu.getQty())).append(" ").append(esc(safe(iu.getUnit())));
                        if (iu.getPhiDays() != null && iu.getPhiDays() > 0) sb.append(" <span class=\"badge badge-warn\">PHI ").append(iu.getPhiDays()).append("d</span>");
                        sb.append("</li>");
                    }
                    sb.append("</ul>");
                }
                sb.append("</td></tr>");
            }
            sb.append("</tbody></table>");
        }

        // ---- PHI compliance ----
        sb.append("<h3>3. PHI (Pre-Harvest Interval) Compliance</h3>");
        if (d.getPhiCompliance() == null || d.getPhiCompliance().getSprayChecks() == null
                || d.getPhiCompliance().getSprayChecks().isEmpty()) {
            sb.append("<div class=\"verdict-mini ok\">No pesticide application recorded. PHI requirement not applicable.</div>");
        } else {
            GapReportData.PhiCompliance phi = d.getPhiCompliance();
            sb.append("<div class=\"verdict-mini ").append(phi.isCompliant() ? "ok" : "fail").append("\">");
            sb.append(phi.isCompliant() ? "COMPLIANT &mdash; all sprays respected PHI before harvest." : "NON-COMPLIANT &mdash; at least one spray was applied within PHI window before harvest.");
            sb.append(" Earliest safe harvest date: <strong>").append(fmtDate(phi.getEarliestSafeDate())).append("</strong>, actual harvest: <strong>").append(fmtDate(phi.getHarvestDate())).append("</strong>");
            sb.append("</div>");
            sb.append("<table class=\"data\"><thead><tr>");
            sb.append("<th>Spray Date</th><th>Input</th><th>Active</th><th>PHI (d)</th><th>Safe From</th><th>Verdict</th>");
            sb.append("</tr></thead><tbody>");
            for (GapReportData.SprayCheck c : phi.getSprayChecks()) {
                sb.append("<tr>");
                sb.append("<td>").append(fmtDate(c.getSprayDate())).append("</td>");
                sb.append("<td><code>").append(esc(c.getInputCode())).append("</code> ").append(esc(c.getInputName())).append("</td>");
                sb.append("<td class=\"dim\">").append(esc(safe(c.getActiveIngredient()))).append("</td>");
                sb.append("<td>").append(c.getPhiDays() == null ? "&mdash;" : c.getPhiDays()).append("</td>");
                sb.append("<td>").append(fmtDate(c.getSafeDate())).append("</td>");
                sb.append("<td>").append(c.isCompliant() ? "<span class=\"badge badge-ok\">OK</span>" : "<span class=\"badge badge-fail\">BREACH</span>").append("</td>");
                sb.append("</tr>");
            }
            sb.append("</tbody></table>");
        }

        // ---- Inbound (input source) ----
        sb.append("<h3>4. Input Source &mdash; Inbound Records</h3>");
        if (t.getInbounds() == null || t.getInbounds().isEmpty()) {
            sb.append("<div class=\"empty\">No traceable input inbound records.</div>");
        } else {
            sb.append("<table class=\"data\"><thead><tr>");
            sb.append("<th>Inbound</th><th>Confirmed</th><th>Source PO</th><th>Items</th>");
            sb.append("</tr></thead><tbody>");
            for (TraceVO.InboundNode ib : t.getInbounds()) {
                sb.append("<tr>");
                sb.append("<td class=\"mono\">").append(esc(ib.getCode())).append("</td>");
                sb.append("<td>").append(fmtDate(ib.getConfirmedAt())).append("</td>");
                sb.append("<td>").append(esc(safe(ib.getSourceCode()))).append("</td>");
                sb.append("<td>");
                if (ib.getItems() != null) {
                    sb.append("<ul class=\"input-list\">");
                    for (TraceVO.InputReceived ir : ib.getItems()) {
                        sb.append("<li><code>").append(esc(ir.getInputItemCode())).append("</code> ").append(esc(ir.getInputItemName())).append(" &middot; ").append(qty(ir.getQty())).append("</li>");
                    }
                    sb.append("</ul>");
                }
                sb.append("</td></tr>");
            }
            sb.append("</tbody></table>");
        }

        // ---- Harvest ----
        if (t.getHarvest() != null) {
            sb.append("<h3>5. Harvest</h3>");
            sb.append("<table class=\"data\"><tbody>");
            sb.append("<tr><td class=\"k\">Code</td><td class=\"mono\">").append(esc(t.getHarvest().getCode())).append("</td>");
            sb.append("<td class=\"k\">Date</td><td>").append(fmtDate(t.getHarvest().getHarvestDate())).append("</td></tr>");
            sb.append("<tr><td class=\"k\">Qty (kg)</td><td><strong>").append(qty(t.getHarvest().getQtyKg())).append("</strong></td>");
            sb.append("<td class=\"k\">Operator</td><td>").append(esc(safe(t.getHarvest().getOperatorName()))).append("</td></tr>");
            if (t.getHarvest().getLocationGps() != null) {
                sb.append("<tr><td class=\"k\">GPS</td><td colspan=\"3\" class=\"dim small\">").append(esc(t.getHarvest().getLocationGps())).append("</td></tr>");
            }
            sb.append("</tbody></table>");
        }

        // ---- QC summary ----
        sb.append("<h3>6. Quality Control Inspections</h3>");
        GapReportData.QcSummary qc = d.getQcSummary();
        if (qc == null || qc.getTotalInspections() == 0) {
            sb.append("<div class=\"empty\">No QC inspections recorded for this batch.</div>");
        } else {
            sb.append("<div class=\"qc-summary\">");
            sb.append("<span class=\"qc-pill ok\">Pass ").append(qc.getPass()).append("</span>");
            sb.append("<span class=\"qc-pill warn\">Conditional ").append(qc.getConditionalPass()).append("</span>");
            sb.append("<span class=\"qc-pill fail\">Fail ").append(qc.getFail()).append("</span>");
            sb.append("<span class=\"qc-pill\">Pending ").append(qc.getPending()).append("</span>");
            sb.append("</div>");
            sb.append("<table class=\"data\"><thead><tr>");
            sb.append("<th>Code</th><th>Type</th><th>Date</th><th>Inspector</th><th>Result</th><th>Remark</th>");
            sb.append("</tr></thead><tbody>");
            for (GapReportData.InspectionEntry e : qc.getInspections()) {
                sb.append("<tr>");
                sb.append("<td class=\"mono\">").append(esc(safe(e.getCode()))).append("</td>");
                sb.append("<td>").append(esc(safe(e.getInspectionType()))).append("</td>");
                sb.append("<td>").append(fmtDate(e.getInspectDate())).append("</td>");
                sb.append("<td>").append(esc(safe(e.getInspectorName()))).append("</td>");
                sb.append("<td><span class=\"badge badge-").append(qcResultClass(e.getResult())).append("\">").append(esc(safe(e.getResult()))).append("</span></td>");
                sb.append("<td class=\"dim\">").append(esc(safe(e.getResultRemark()))).append("</td>");
                sb.append("</tr>");
            }
            sb.append("</tbody></table>");
        }

        // ---- Packing ----
        sb.append("<h3>7. Packing</h3>");
        if (t.getPackings() == null || t.getPackings().isEmpty()) {
            sb.append("<div class=\"empty\">No packing records.</div>");
        } else {
            sb.append("<table class=\"data\"><thead><tr>");
            sb.append("<th>Code</th><th>Date</th><th>SKU</th><th>Qty</th>");
            sb.append("</tr></thead><tbody>");
            for (TraceVO.PackingNode p : t.getPackings()) {
                sb.append("<tr><td class=\"mono\">").append(esc(p.getCode())).append("</td>");
                sb.append("<td>").append(fmtDate(p.getPackDate())).append("</td>");
                sb.append("<td>").append(esc(p.getSkuCode())).append(" <span class=\"dim\">").append(esc(safe(p.getSkuName()))).append("</span></td>");
                sb.append("<td><strong>").append(qty(p.getQty())).append("</strong> ").append(esc(safe(p.getUnit()))).append("</td>");
                sb.append("</tr>");
            }
            sb.append("</tbody></table>");
        }

        // ---- Shipments ----
        sb.append("<h3>8. Sales Orders (Shipments)</h3>");
        if (t.getOrders() == null || t.getOrders().isEmpty()) {
            sb.append("<div class=\"empty\">No sales orders linked.</div>");
        } else {
            sb.append("<table class=\"data\"><thead><tr>");
            sb.append("<th>Order</th><th>Date</th><th>Customer</th><th>Status</th>");
            sb.append("</tr></thead><tbody>");
            for (TraceVO.OrderNode o : t.getOrders()) {
                sb.append("<tr><td class=\"mono\">").append(esc(o.getCode())).append("</td>");
                sb.append("<td>").append(fmtDate(o.getOrderDate())).append("</td>");
                sb.append("<td>").append(esc(safe(o.getCustomerName()))).append("</td>");
                sb.append("<td>").append(esc(safe(o.getStatus()))).append("</td></tr>");
            }
            sb.append("</tbody></table>");
        }

        // ---- Complaints / recalls ----
        List<GapReportData.ComplaintEntry> compls = d.getComplaints();
        List<GapReportData.RecallEntry> recalls = d.getRecalls();
        if ((compls != null && !compls.isEmpty()) || (recalls != null && !recalls.isEmpty())) {
            sb.append("<h3>9. Food-Safety Events</h3>");
            if (compls != null && !compls.isEmpty()) {
                sb.append("<div class=\"label\">Complaints</div>");
                sb.append("<table class=\"data\"><thead><tr>");
                sb.append("<th>Code</th><th>Reported</th><th>Category</th><th>Severity</th><th>Status</th><th>Description</th>");
                sb.append("</tr></thead><tbody>");
                for (GapReportData.ComplaintEntry c : compls) {
                    sb.append("<tr><td class=\"mono\">").append(esc(c.getCode())).append("</td>");
                    sb.append("<td>").append(fmtDateTime(c.getReportedAt())).append("</td>");
                    sb.append("<td>").append(esc(safe(c.getCategory()))).append("</td>");
                    sb.append("<td>").append(esc(safe(c.getSeverity()))).append("</td>");
                    sb.append("<td>").append(esc(safe(c.getStatus()))).append("</td>");
                    sb.append("<td class=\"dim\">").append(esc(safe(c.getDescription()))).append("</td></tr>");
                }
                sb.append("</tbody></table>");
            }
            if (recalls != null && !recalls.isEmpty()) {
                sb.append("<div class=\"label\" style=\"margin-top:8px\">Recalls</div>");
                sb.append("<table class=\"data\"><thead><tr>");
                sb.append("<th>Code</th><th>Triggered</th><th>Status</th><th>Affected Orders</th><th>Reason</th>");
                sb.append("</tr></thead><tbody>");
                for (GapReportData.RecallEntry r : recalls) {
                    sb.append("<tr><td class=\"mono\">").append(esc(r.getCode())).append("</td>");
                    sb.append("<td>").append(fmtDateTime(r.getTriggeredAt())).append("</td>");
                    sb.append("<td>").append(esc(safe(r.getStatus()))).append("</td>");
                    sb.append("<td>").append(r.getAffectedOrderCount() == null ? "0" : r.getAffectedOrderCount()).append("</td>");
                    sb.append("<td class=\"dim\">").append(esc(safe(r.getReason()))).append("</td></tr>");
                }
                sb.append("</tbody></table>");
            }
        }

        // ---- Auditor sign-off ----
        sb.append("<div class=\"signoff\">");
        sb.append("<div class=\"signoff-block\"><div class=\"label\">Auditor Name</div><div class=\"line\"></div></div>");
        sb.append("<div class=\"signoff-block\"><div class=\"label\">Signature</div><div class=\"line\"></div></div>");
        sb.append("<div class=\"signoff-block\"><div class=\"label\">Date</div><div class=\"line\"></div></div>");
        sb.append("</div>");

        sb.append("<div class=\"footer\">");
        sb.append("Generated by 2Africa AgriOS on ").append(LocalDateTime.now().format(DATETIME_FMT)).append(". ");
        sb.append("Reproduction of this report requires verification against original system records.");
        sb.append("</div>");
        sb.append("</body></html>");
        return sb.toString();
    }

    // ----- helpers -----
    private static String verdictClass(String v) {
        return switch (v == null ? "" : v) {
            case "COMPLIANT" -> "ok";
            case "FLAGGED" -> "warn";
            case "NON_COMPLIANT" -> "fail";
            default -> "neutral";
        };
    }

    private static String qcResultClass(String r) {
        return switch (r == null ? "" : r) {
            case "pass" -> "ok";
            case "fail" -> "fail";
            case "conditional_pass" -> "warn";
            default -> "neutral";
        };
    }

    private static String actClass(String a) {
        return switch (a == null ? "" : a) {
            case "spray" -> "fail";
            case "fertilize" -> "ok";
            case "sow", "harvest" -> "neutral";
            default -> "warn";
        };
    }

    private static String fmtDate(LocalDate d) { return d == null ? "&mdash;" : d.format(DATE_FMT); }
    private static String fmtDateTime(LocalDateTime t) { return t == null ? "&mdash;" : t.format(DATETIME_FMT); }
    private static String qty(BigDecimal v) { return v == null ? "0" : v.stripTrailingZeros().toPlainString(); }
    private static String safe(String s) { return s == null ? "" : s; }
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static final String CSS = """
            <style>
              @page { size: A4; margin: 14mm 12mm; }
              body { font-family: Helvetica, Arial, sans-serif; font-size: 9.5pt; color: #1f2329; }
              h3 { font-size: 11pt; color: #1f7a35; border-bottom: 1px solid #d8e6dc;
                   padding-bottom: 4px; margin: 14px 0 6px 0; }
              .head { display: table; width: 100%; border-bottom: 2px solid #1f7a35; padding-bottom: 8px; }
              .head .brand, .head .doc-title { display: table-cell; vertical-align: middle; }
              .head .brand { font-size: 13pt; font-weight: bold; color: #1f7a35; }
              .head .logo { display: inline-block; background: #1f7a35; color: #fff;
                            padding: 2px 6px; border-radius: 4px; margin-right: 6px; font-weight: 700; }
              .head .doc-title { text-align: right; font-size: 14pt; font-weight: 700; letter-spacing: 1px; }
              .verdict { display: flex; justify-content: space-between; align-items: center;
                         padding: 10px 14px; border-radius: 6px; margin: 10px 0;
                         font-size: 11pt; font-weight: 700; }
              .verdict.ok      { background: #ecf9ef; color: #15803d; border: 1px solid #b3d8be; }
              .verdict.warn    { background: #fff7e6; color: #b88230; border: 1px solid #f7c873; }
              .verdict.fail    { background: #fdecec; color: #b91c1c; border: 1px solid #fcd7d2; }
              .verdict.neutral { background: #f5f7fa; color: #606266; border: 1px solid #e4e7ed; }
              .verdict-label { font-size: 9pt; text-transform: uppercase; letter-spacing: 1px; opacity: 0.7; }
              .verdict-value { font-size: 14pt; }
              .verdict-mini { padding: 8px 12px; border-radius: 4px; margin: 6px 0 10px; font-size: 9.5pt; }
              .verdict-mini.ok   { background: #ecf9ef; color: #15803d; border-left: 3px solid #15803d; }
              .verdict-mini.fail { background: #fdecec; color: #b91c1c; border-left: 3px solid #b91c1c; }
              .meta { width: 100%; margin-top: 8px; border-collapse: collapse; }
              .meta td { vertical-align: top; padding: 6px 10px; }
              .meta .meta-l { width: 60%; background: #f7faf8; border-left: 3px solid #1f7a35; }
              .meta .meta-r { width: 40%; background: #f7faf8; border-left: 3px solid #909399; }
              .label { font-size: 7pt; color: #909399; text-transform: uppercase; letter-spacing: 1px; }
              .big   { font-size: 12pt; font-weight: 700; margin: 2px 0; }
              .dim   { color: #606266; }
              .small { font-size: 8pt; }
              .mono  { font-family: 'Courier New', monospace; color: #1f7a35; font-weight: 600; }
              .data  { width: 100%; border-collapse: collapse; margin-top: 4px; }
              .data th, .data td { border: 1px solid #e4e7ed; padding: 4px 6px; font-size: 8.5pt; vertical-align: top; }
              .data th { background: #f5f7fa; text-align: left; font-weight: 600; }
              .data .k { background: #fafbfc; color: #606266; font-size: 7.5pt; text-transform: uppercase;
                         letter-spacing: 0.5px; width: 14%; }
              .empty { text-align: center; padding: 12px; color: #909399; font-style: italic;
                       border: 1px dashed #e4e7ed; border-radius: 4px; }
              .input-list { padding-left: 14px; margin: 2px 0; font-size: 8pt; }
              .badge { padding: 1px 5px; border-radius: 3px; font-size: 7.5pt; font-weight: 600;
                       letter-spacing: 0.3px; display: inline-block; }
              .badge-ok      { background: #ecf9ef; color: #15803d; }
              .badge-warn    { background: #fff7e6; color: #b88230; }
              .badge-fail    { background: #fdecec; color: #b91c1c; }
              .badge-neutral { background: #f0f2f5; color: #606266; }
              .qc-summary { display: flex; gap: 6px; margin: 4px 0 8px; }
              .qc-pill { padding: 3px 10px; border-radius: 12px; background: #f5f7fa; font-size: 8.5pt; }
              .qc-pill.ok   { background: #ecf9ef; color: #15803d; }
              .qc-pill.warn { background: #fff7e6; color: #b88230; }
              .qc-pill.fail { background: #fdecec; color: #b91c1c; }
              .signoff { display: flex; gap: 16px; margin-top: 24px; }
              .signoff-block { flex: 1; }
              .signoff-block .line { border-bottom: 1px solid #94a3b8; height: 24px; margin-top: 4px; }
              .footer { margin-top: 18px; padding-top: 8px; border-top: 1px dashed #d8e6dc;
                        font-size: 7.5pt; color: #909399; }
            </style>
            """;
}
