package ai.toafrica.agrios.finance.service;

import ai.toafrica.agrios.finance.vo.CustomerStatementVO;
import ai.toafrica.agrios.finance.vo.StatementOrderLine;
import ai.toafrica.agrios.finance.vo.StatementPaymentLine;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 客户对账单 PDF 渲染 - Sprint 16.5.
 *   - 用 OpenHtmlToPdf 把 HTML 模板渲染为 PDF
 *   - HTML 全部内联, 无外部模板引擎 (Thymeleaf 等), 减少依赖
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatementPdfService {

    private final CustomerStatementService statementService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public byte[] renderPdf(Long customerId, LocalDate from, LocalDate to) {
        CustomerStatementVO vo = statementService.build(customerId, from, to);
        String html = buildHtml(vo);
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder b = new PdfRendererBuilder();
            b.useFastMode();
            b.withHtmlContent(html, null);
            b.toStream(os);
            b.run();
            byte[] bytes = os.toByteArray();
            log.info("[StatementPdf] customer={} period={}~{} pdf={}KB",
                    vo.getCustomerCode(), from, to, bytes.length / 1024);
            return bytes;
        } catch (Exception e) {
            throw new RuntimeException("Failed to render statement PDF", e);
        }
    }

    // ---------------------------------------------------------------------
    // HTML 模板
    // ---------------------------------------------------------------------
    private String buildHtml(CustomerStatementVO vo) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html xmlns=\"http://www.w3.org/1999/xhtml\"><head>");
        sb.append("<meta charset=\"UTF-8\"/>");
        sb.append("<title>Statement of Account</title>");
        sb.append(CSS);
        sb.append("</head><body>");

        // ---- Header ----
        sb.append("<div class=\"head\">");
        sb.append("<div class=\"brand\"><span class=\"logo\">2A</span> 2Africa AgriOS</div>");
        sb.append("<div class=\"doc-title\">STATEMENT OF ACCOUNT</div>");
        sb.append("</div>");

        // ---- Customer + period ----
        sb.append("<table class=\"meta\"><tr>");
        sb.append("<td class=\"meta-l\">");
        sb.append("<div class=\"label\">Bill To</div>");
        sb.append("<div class=\"big\">").append(esc(vo.getCustomerName())).append("</div>");
        sb.append("<div class=\"dim\">").append(esc(vo.getCustomerCode()));
        if (vo.getCustomerType() != null) sb.append(" &#183; ").append(esc(vo.getCustomerType()));
        sb.append("</div>");
        if (vo.getContactName() != null || vo.getContactPhone() != null) {
            sb.append("<div class=\"dim\">");
            if (vo.getContactName() != null)  sb.append(esc(vo.getContactName()));
            if (vo.getContactPhone() != null) sb.append(" &#183; ").append(esc(vo.getContactPhone()));
            sb.append("</div>");
        }
        if (vo.getCreditDays() != null) {
            sb.append("<div class=\"dim\">Payment terms: ");
            sb.append(esc(vo.getPaymentTerms() != null ? vo.getPaymentTerms() : ""));
            sb.append(" (").append(vo.getCreditDays()).append(" days)</div>");
        }
        sb.append("</td><td class=\"meta-r\">");
        sb.append("<div class=\"label\">Statement Period</div>");
        sb.append("<div class=\"big\">").append(vo.getFromDate()).append(" &#8594; ").append(vo.getToDate()).append("</div>");
        sb.append("<div class=\"dim\">Generated ").append(vo.getGeneratedAt()).append("</div>");
        sb.append("</td></tr></table>");

        // ---- Balance summary ----
        sb.append("<div class=\"summary\">");
        sb.append(box("Opening Balance",  vo.getOpeningBalance(), "neutral"));
        sb.append(box("Period Sales",     vo.getPeriodSales(),    "positive"));
        sb.append(box("Period Payments",  vo.getPeriodPayments(), "credit"));
        sb.append(box("Closing Balance",  vo.getClosingBalance(), "highlight"));
        sb.append("</div>");

        // ---- Orders ----
        sb.append("<h3>Orders in period</h3>");
        sb.append("<table class=\"data\"><thead><tr>");
        sb.append("<th>Date</th><th>Order #</th><th>Due</th>");
        sb.append("<th class=\"r\">Total</th><th class=\"r\">Paid</th><th class=\"r\">Outstanding</th>");
        sb.append("<th>Currency</th><th>Status</th>");
        sb.append("</tr></thead><tbody>");
        List<StatementOrderLine> orders = vo.getOrders();
        if (orders == null || orders.isEmpty()) {
            sb.append("<tr><td colspan=\"8\" class=\"empty\">No orders in this period.</td></tr>");
        } else {
            for (StatementOrderLine r : orders) {
                sb.append("<tr>");
                sb.append("<td>").append(fmtDate(r.getOrderDate())).append("</td>");
                sb.append("<td class=\"mono\">").append(esc(r.getOrderCode())).append("</td>");
                sb.append("<td>").append(fmtDate(r.getDueDate())).append("</td>");
                sb.append("<td class=\"r\">").append(money(r.getTotalAmount())).append("</td>");
                sb.append("<td class=\"r\">").append(money(r.getPaidAmount())).append("</td>");
                sb.append("<td class=\"r strong\">").append(money(r.getOutstanding())).append("</td>");
                sb.append("<td>").append(esc(r.getCurrency())).append("</td>");
                sb.append("<td><span class=\"badge ").append(tagClass(r.getPaymentStatus())).append("\">")
                        .append(esc(r.getPaymentStatus())).append("</span></td>");
                sb.append("</tr>");
            }
        }
        sb.append("</tbody></table>");

        // ---- Payments ----
        sb.append("<h3>Payments in period</h3>");
        sb.append("<table class=\"data\"><thead><tr>");
        sb.append("<th>Date</th><th>Payment #</th><th>Order #</th>");
        sb.append("<th class=\"r\">Amount</th><th>Cur</th><th class=\"r\">Amount (KES)</th>");
        sb.append("<th>Method</th><th>Reference</th>");
        sb.append("</tr></thead><tbody>");
        List<StatementPaymentLine> payments = vo.getPayments();
        if (payments == null || payments.isEmpty()) {
            sb.append("<tr><td colspan=\"8\" class=\"empty\">No payments received in this period.</td></tr>");
        } else {
            for (StatementPaymentLine p : payments) {
                sb.append("<tr>");
                sb.append("<td>").append(fmtDate(p.getPaymentDate())).append("</td>");
                sb.append("<td class=\"mono\">").append(esc(p.getPaymentCode())).append("</td>");
                sb.append("<td class=\"mono\">").append(esc(p.getOrderCode())).append("</td>");
                sb.append("<td class=\"r\">").append(money(p.getAmount())).append("</td>");
                sb.append("<td>").append(esc(p.getCurrency())).append("</td>");
                sb.append("<td class=\"r strong\">").append(money(p.getAmountKes())).append("</td>");
                sb.append("<td>").append(esc(p.getMethod())).append("</td>");
                sb.append("<td class=\"dim\">").append(esc(p.getReferenceNo())).append("</td>");
                sb.append("</tr>");
            }
        }
        sb.append("</tbody></table>");

        // ---- Aging summary ----
        sb.append("<h3>Aging as of ").append(vo.getToDate()).append("</h3>");
        sb.append("<table class=\"aging\"><thead><tr>");
        sb.append("<th>0&#8211;7 days</th>");
        sb.append("<th>8&#8211;14 days</th>");
        sb.append("<th>15&#8211;30 days</th>");
        sb.append("<th class=\"danger\">30+ days</th>");
        sb.append("<th>Total Outstanding</th>");
        sb.append("</tr></thead><tbody><tr>");
        sb.append("<td>").append(money(vo.getAging0to7())).append("</td>");
        sb.append("<td>").append(money(vo.getAging8to14())).append("</td>");
        sb.append("<td>").append(money(vo.getAging15to30())).append("</td>");
        sb.append("<td class=\"danger strong\">").append(money(vo.getAging30Plus())).append("</td>");
        sb.append("<td class=\"strong\">").append(money(vo.getClosingBalance())).append("</td>");
        sb.append("</tr></tbody></table>");

        // ---- Footer ----
        sb.append("<div class=\"footer\">");
        sb.append("All amounts in KES (Kenyan Shilling) unless stated otherwise.<br/>");
        sb.append("Generated by 2Africa AgriOS on ").append(vo.getGeneratedAt()).append(".<br/>");
        sb.append("For queries, please contact our finance team.");
        sb.append("</div>");

        sb.append("</body></html>");
        return sb.toString();
    }

    private static String box(String label, BigDecimal v, String cssClass) {
        return "<div class=\"box " + cssClass + "\">"
             + "<div class=\"box-label\">" + label + "</div>"
             + "<div class=\"box-value\">" + money(v) + "</div>"
             + "<div class=\"box-cur\">KES</div>"
             + "</div>";
    }

    private static String tagClass(String paymentStatus) {
        if (paymentStatus == null) return "neutral";
        return switch (paymentStatus) {
            case "paid"    -> "positive";
            case "partial" -> "warn";
            case "unpaid"  -> "danger";
            default -> "neutral";
        };
    }

    private static String fmtDate(LocalDate d) {
        return d == null ? "&#8212;" : d.format(DATE_FMT);
    }

    private static String money(BigDecimal v) {
        if (v == null) return "0.00";
        return String.format("%,.2f", v);
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    // ---------------------------------------------------------------------
    // CSS — 内联在 <head>
    // ---------------------------------------------------------------------
    private static final String CSS = """
            <style>
              @page { size: A4; margin: 14mm 12mm; }
              body { font-family: Helvetica, Arial, sans-serif; font-size: 10pt; color: #1f2329; }
              h3 { font-size: 11pt; color: #1f7a35; border-bottom: 1px solid #d8e6dc;
                   padding-bottom: 4px; margin: 16px 0 6px 0; }
              .head { display: table; width: 100%; border-bottom: 2px solid #1f7a35; padding-bottom: 10px; }
              .head .brand, .head .doc-title { display: table-cell; vertical-align: middle; }
              .head .brand { font-size: 14pt; font-weight: bold; color: #1f7a35; }
              .head .logo { display: inline-block; background: #1f7a35; color: #fff;
                            padding: 2px 6px; border-radius: 4px; margin-right: 6px; font-weight: 700; }
              .head .doc-title { text-align: right; font-size: 16pt; font-weight: 700; color: #1f2329; letter-spacing: 1px; }
              .meta { width: 100%; margin-top: 14px; border-collapse: collapse; }
              .meta td { vertical-align: top; padding: 6px 8px; }
              .meta .meta-l { width: 60%; background: #f7faf8; border-left: 3px solid #1f7a35; }
              .meta .meta-r { width: 40%; background: #f7faf8; border-left: 3px solid #909399; }
              .label { font-size: 8pt; color: #909399; text-transform: uppercase; letter-spacing: 1px; }
              .big   { font-size: 12pt; font-weight: 700; margin: 2px 0; }
              .dim   { color: #606266; font-size: 9pt; }
              .summary { display: table; width: 100%; table-layout: fixed; margin: 14px 0; border-spacing: 6px 0; }
              .summary .box { display: table-cell; padding: 8px 10px; background: #f7faf8;
                              border-radius: 4px; border: 1px solid #e4e7ed; }
              .summary .box.positive  { background: #ecf9ef; border-color: #b3d8be; }
              .summary .box.credit    { background: #ecf3f9; border-color: #b3c8d8; }
              .summary .box.highlight { background: #fff5e6; border-color: #f7c873; }
              .summary .box.neutral   { }
              .box-label { font-size: 8pt; color: #909399; text-transform: uppercase; }
              .box-value { font-size: 13pt; font-weight: 700; color: #1f2329; margin: 2px 0; }
              .box-cur   { font-size: 8pt; color: #909399; }
              .data, .aging { width: 100%; border-collapse: collapse; margin-top: 4px; }
              .data th, .data td, .aging th, .aging td {
                border: 1px solid #e4e7ed; padding: 5px 7px; font-size: 9pt;
              }
              .data th { background: #f5f7fa; text-align: left; font-weight: 600; }
              .aging th { background: #f5f7fa; text-align: center; font-weight: 600; }
              .aging td { text-align: right; padding: 8px 12px; font-size: 10pt; }
              .data .r, .aging .r { text-align: right; }
              .data .mono { font-family: 'Courier New', monospace; color: #1f7a35; font-weight: 600; }
              .data .empty { text-align: center; color: #909399; padding: 14px; font-style: italic; }
              .strong { font-weight: 700; }
              .danger { color: #c45656; }
              .badge { padding: 1px 6px; border-radius: 3px; font-size: 8pt; font-weight: 600; }
              .badge.positive { background: #ecf9ef; color: #1f7a35; }
              .badge.warn     { background: #fff5e6; color: #b88230; }
              .badge.danger   { background: #fdecec; color: #c45656; }
              .badge.neutral  { background: #f0f2f5; color: #606266; }
              .footer { margin-top: 20px; padding-top: 10px; border-top: 1px dashed #d8e6dc;
                        font-size: 8pt; color: #909399; line-height: 1.6; }
            </style>
            """;
}
