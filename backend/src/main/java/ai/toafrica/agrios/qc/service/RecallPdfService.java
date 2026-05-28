package ai.toafrica.agrios.qc.service;

import ai.toafrica.agrios.qc.vo.RecallDetailVO;
import ai.toafrica.agrios.qc.vo.RecallVO;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Recall PDF report (Sprint 27).
 *
 * Single-page A4 report containing:
 *   - Header with recall code + status badge
 *   - Batch + product info
 *   - Reason narrative
 *   - Affected orders / customers table
 *   - Notification status snapshot at render time
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecallPdfService {

    private final RecallService recallService;
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] renderPdf(Long recallId) {
        RecallDetailVO detail = recallService.detail(recallId);
        String html = buildHtml(detail);
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder b = new PdfRendererBuilder();
            b.useFastMode();
            b.withHtmlContent(html, null);
            b.toStream(os);
            b.run();
            byte[] bytes = os.toByteArray();
            log.info("[RecallPdf] recall={} pdf={}KB",
                    detail.getRecall().getCode(), bytes.length / 1024);
            return bytes;
        } catch (Exception e) {
            throw new RuntimeException("Failed to render recall PDF", e);
        }
    }

    private String buildHtml(RecallDetailVO d) {
        RecallVO r = d.getRecall();
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html xmlns=\"http://www.w3.org/1999/xhtml\"><head>");
        sb.append("<meta charset=\"UTF-8\"/>");
        sb.append("<title>Recall Report — ").append(esc(r.getCode())).append("</title>");
        sb.append(CSS);
        sb.append("</head><body>");

        // ---- Header ----
        sb.append("<div class=\"head\">");
        sb.append("<div class=\"brand\"><span class=\"logo\">2A</span> 2Africa AgriOS</div>");
        sb.append("<div class=\"doc-title\">RECALL REPORT</div>");
        sb.append("</div>");

        // ---- Recall summary ----
        sb.append("<table class=\"meta\"><tr>");
        sb.append("<td class=\"meta-l\">");
        sb.append("<div class=\"label\">Recall Code</div>");
        sb.append("<div class=\"big mono\">").append(esc(r.getCode())).append("</div>");
        sb.append("<div class=\"dim\">Triggered at ").append(fmtDateTime(r.getTriggeredAt())).append("</div>");
        if (r.getInitiatedByName() != null) {
            sb.append("<div class=\"dim\">By ").append(esc(r.getInitiatedByName())).append("</div>");
        }
        if (r.getSourceComplaintCode() != null) {
            sb.append("<div class=\"dim\">Source complaint: ").append(esc(r.getSourceComplaintCode())).append("</div>");
        }
        sb.append("</td><td class=\"meta-r\">");
        sb.append("<div class=\"label\">Status</div>");
        sb.append("<div class=\"big\"><span class=\"badge ").append(statusClass(r.getStatus())).append("\">")
                .append(esc(r.getStatus().toUpperCase())).append("</span></div>");
        sb.append("<div class=\"dim\">Scope: ").append(esc(r.getScope())).append("</div>");
        sb.append("</td></tr></table>");

        // ---- Product info ----
        sb.append("<h3>Recalled Batch</h3>");
        sb.append("<table class=\"data\"><tbody>");
        sb.append("<tr><td class=\"k\">Batch Code</td><td class=\"mono\">").append(esc(r.getBatchCode())).append("</td>");
        sb.append("<td class=\"k\">Crop / Variety</td><td>")
                .append(esc(r.getCropName())).append(r.getVarietyName() == null ? "" : " — " + esc(r.getVarietyName()))
                .append("</td></tr>");
        sb.append("<tr><td class=\"k\">Affected Qty</td><td class=\"strong\">").append(qty(r.getAffectedQty())).append("</td>");
        sb.append("<td class=\"k\">Affected Orders</td><td><strong>").append(r.getAffectedOrderCount()).append("</strong> across ")
                .append(r.getAffectedCustomerCount()).append(" customer(s)</td></tr>");
        sb.append("</tbody></table>");

        // ---- Reason ----
        sb.append("<h3>Reason</h3>");
        sb.append("<div class=\"reason\">").append(esc(r.getReason()).replace("\n", "<br/>")).append("</div>");

        // ---- Affected orders ----
        sb.append("<h3>Affected Orders &amp; Customers</h3>");
        sb.append("<table class=\"data\"><thead><tr>");
        sb.append("<th>#</th><th>Order</th><th>Customer</th><th class=\"r\">Qty</th>");
        sb.append("<th>Delivered</th><th>Notified</th><th>Notified By</th>");
        sb.append("</tr></thead><tbody>");
        List<RecallDetailVO.AffectedOrder> rows = d.getAffectedOrders();
        if (rows == null || rows.isEmpty()) {
            sb.append("<tr><td colspan=\"7\" class=\"empty\">No downstream orders. Inventory quarantined only.</td></tr>");
        } else {
            int i = 1;
            for (RecallDetailVO.AffectedOrder o : rows) {
                sb.append("<tr>");
                sb.append("<td>").append(i++).append("</td>");
                sb.append("<td class=\"mono\">").append(esc(o.getOrderCode())).append("</td>");
                sb.append("<td>").append(esc(o.getCustomerName())).append("</td>");
                sb.append("<td class=\"r strong\">").append(qty(o.getQty())).append(" ").append(esc(o.getUnit())).append("</td>");
                sb.append("<td>").append(fmtDateTime(o.getDeliveredAt())).append("</td>");
                if (o.getNotifiedAt() != null) {
                    sb.append("<td><span class=\"badge positive\">").append(fmtDateTime(o.getNotifiedAt())).append("</span></td>");
                } else {
                    sb.append("<td><span class=\"badge danger\">PENDING</span></td>");
                }
                sb.append("<td>").append(o.getNotifiedByName() == null ? "&#8212;" : esc(o.getNotifiedByName())).append("</td>");
                sb.append("</tr>");
            }
        }
        sb.append("</tbody></table>");

        // ---- Close info ----
        if (r.getClosedAt() != null) {
            sb.append("<h3>Closure</h3>");
            sb.append("<div class=\"reason\">");
            sb.append("Closed at ").append(fmtDateTime(r.getClosedAt()));
            if (r.getClosedByName() != null) sb.append(" by ").append(esc(r.getClosedByName()));
            sb.append(".");
            if (r.getClosedRemark() != null) {
                sb.append("<br/><span class=\"dim\">").append(esc(r.getClosedRemark())).append("</span>");
            }
            sb.append("</div>");
        }

        // ---- Footer ----
        sb.append("<div class=\"footer\">");
        sb.append("This recall report is generated by 2Africa AgriOS on ").append(LocalDateTime.now().format(DATETIME_FMT)).append(".<br/>");
        sb.append("Tampering with recall records is prohibited under farm food-safety policy.");
        sb.append("</div>");

        sb.append("</body></html>");
        return sb.toString();
    }

    private static String fmtDateTime(LocalDateTime t) {
        return t == null ? "&#8212;" : t.format(DATETIME_FMT);
    }

    private static String qty(BigDecimal v) {
        if (v == null) return "0";
        return v.stripTrailingZeros().toPlainString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String statusClass(String status) {
        if (status == null) return "neutral";
        return switch (status) {
            case "initiated"          -> "danger";
            case "quarantined"        -> "warn";
            case "customers_notified" -> "positive";
            case "closed"             -> "neutral";
            default -> "neutral";
        };
    }

    private static final String CSS = """
            <style>
              @page { size: A4; margin: 14mm 12mm; }
              body { font-family: Helvetica, Arial, sans-serif; font-size: 10pt; color: #1f2329; }
              h3 { font-size: 11pt; color: #b91c1c; border-bottom: 1px solid #fcd7d2;
                   padding-bottom: 4px; margin: 16px 0 6px 0; }
              .head { display: table; width: 100%; border-bottom: 2px solid #b91c1c; padding-bottom: 10px; }
              .head .brand, .head .doc-title { display: table-cell; vertical-align: middle; }
              .head .brand { font-size: 14pt; font-weight: bold; color: #1f7a35; }
              .head .logo { display: inline-block; background: #1f7a35; color: #fff;
                            padding: 2px 6px; border-radius: 4px; margin-right: 6px; font-weight: 700; }
              .head .doc-title { text-align: right; font-size: 16pt; font-weight: 700;
                                 color: #b91c1c; letter-spacing: 1px; }
              .meta { width: 100%; margin-top: 14px; border-collapse: collapse; }
              .meta td { vertical-align: top; padding: 6px 8px; }
              .meta .meta-l { width: 60%; background: #fff5f5; border-left: 3px solid #b91c1c; }
              .meta .meta-r { width: 40%; background: #fff5f5; border-left: 3px solid #909399; }
              .label { font-size: 8pt; color: #909399; text-transform: uppercase; letter-spacing: 1px; }
              .big   { font-size: 12pt; font-weight: 700; margin: 2px 0; }
              .dim   { color: #606266; font-size: 9pt; }
              .mono  { font-family: 'Courier New', monospace; color: #b91c1c; font-weight: 600; }
              .reason { background: #fffbeb; border: 1px solid #f7c873; border-radius: 4px;
                        padding: 10px 14px; line-height: 1.5; font-size: 10pt; }
              .data { width: 100%; border-collapse: collapse; margin-top: 4px; }
              .data th, .data td { border: 1px solid #e4e7ed; padding: 5px 7px; font-size: 9pt; }
              .data th { background: #f5f7fa; text-align: left; font-weight: 600; }
              .data .k  { background: #fafbfc; color: #606266; font-size: 8pt; text-transform: uppercase;
                          letter-spacing: 0.5px; width: 18%; }
              .data .r  { text-align: right; }
              .data .empty { text-align: center; color: #909399; padding: 14px; font-style: italic; }
              .data .strong { font-weight: 700; }
              .badge { padding: 2px 8px; border-radius: 3px; font-size: 8pt; font-weight: 600;
                       letter-spacing: 0.5px; }
              .badge.positive { background: #ecf9ef; color: #1f7a35; }
              .badge.warn     { background: #fff5e6; color: #b88230; }
              .badge.danger   { background: #fdecec; color: #b91c1c; }
              .badge.neutral  { background: #f0f2f5; color: #606266; }
              .footer { margin-top: 20px; padding-top: 10px; border-top: 1px dashed #fcd7d2;
                        font-size: 8pt; color: #909399; line-height: 1.6; }
            </style>
            """;
}
