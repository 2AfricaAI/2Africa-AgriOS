package ai.toafrica.agrios.service.service;

import ai.toafrica.agrios.service.config.DigestProperties;
import ai.toafrica.agrios.service.vo.AgentLeaderboardVO;
import ai.toafrica.agrios.service.vo.AnalyticsOverviewVO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Sprint 50e -- weekly CS digest email.
 *
 * <p>Builds a self-contained HTML body from the existing
 * AnalyticsService overview + leaderboard, then sends it via
 * {@link JavaMailSender}. Runs on a Spring cron (Monday 06:00 by
 * default) and can also be triggered manually from
 * {@code DigestController}.</p>
 *
 * <p>SMTP creds live under {@code spring.mail.*} (autoconfigured).
 * Digest behaviour lives under {@code agrios.digest.*}
 * ({@link DigestProperties}).</p>
 *
 * <p>The whole bean is gated on {@code agrios.digest.enabled=true}
 * so a deploy without SMTP creds does not crash at startup.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "agrios.digest", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class WeeklyDigestService {

    private final AnalyticsService analyticsService;
    private final DigestProperties props;
    /** Optional because a fresh dev box may not have configured SMTP yet. */
    @Autowired(required = false)
    private JavaMailSender mailSender;

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    @PostConstruct
    void init() {
        log.info("[digest] enabled cron='{}' recipients={} windowDays={}",
                props.getCron(), props.getRecipients().size(), props.getWindowDays());
    }

    // ------------------------------------------------------------------
    // Scheduled trigger -- Monday 06:00 server time by default.
    // ------------------------------------------------------------------
    @Scheduled(cron = "${agrios.digest.cron:0 0 6 ? * MON}")
    public void scheduledSend() {
        if (props.getRecipients().isEmpty()) {
            log.info("[digest] skipped -- no recipients configured");
            return;
        }
        try {
            sendNow();
        } catch (Exception e) {
            log.error("[digest] scheduled send FAILED", e);
        }
    }

    // ------------------------------------------------------------------
    // Manual entry points (called by DigestController).
    // ------------------------------------------------------------------

    /**
     * Render the HTML body without sending. Useful for preview /
     * troubleshooting in the dashboard.
     */
    public String preview() {
        AnalyticsOverviewVO overview = analyticsService.overview(props.getWindowDays());
        AgentLeaderboardVO leaderboard = analyticsService.agentLeaderboard(props.getWindowDays());
        return buildHtml(overview, leaderboard);
    }

    /**
     * Build + send right now. Returns the recipient list for the log /
     * UI confirmation.
     */
    public List<String> sendNow() {
        if (mailSender == null) {
            throw new IllegalStateException("JavaMailSender not configured -- set spring.mail.*");
        }
        if (props.getRecipients().isEmpty()) {
            throw new IllegalStateException("No recipients configured -- set agrios.digest.recipients");
        }

        AnalyticsOverviewVO overview = analyticsService.overview(props.getWindowDays());
        AgentLeaderboardVO leaderboard = analyticsService.agentLeaderboard(props.getWindowDays());
        String html = buildHtml(overview, leaderboard);
        String subject = buildSubject();

        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, "UTF-8");
            helper.setFrom(props.getFrom());
            helper.setTo(props.getRecipients().toArray(new String[0]));
            if (!props.getCc().isEmpty()) {
                helper.setCc(props.getCc().toArray(new String[0]));
            }
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(mime);
            log.info("[digest] sent subject='{}' to={}", subject, props.getRecipients());
            return List.copyOf(props.getRecipients());
        } catch (Exception e) {
            log.error("[digest] send failed", e);
            throw new RuntimeException("Failed to send digest email", e);
        }
    }

    // ------------------------------------------------------------------
    // Rendering -- locale-aware, no Thymeleaf dependency.
    // ------------------------------------------------------------------

    private String buildSubject() {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(props.getWindowDays());
        return String.format("%s %s -> %s",
                props.getSubjectPrefix(),
                from.format(ISO_DATE),
                today.format(ISO_DATE));
    }

    /**
     * Hand-rolled HTML with inline styles -- most email clients strip
     * external CSS, so inline is the only safe choice.
     */
    private String buildHtml(AnalyticsOverviewVO overview, AgentLeaderboardVO leaderboard) {
        Locale loc = Locale.forLanguageTag(props.getLocale());
        I18n t = I18n.forLocale(loc);

        StringBuilder sb = new StringBuilder(8192);
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body ")
          .append("style='font-family:Inter,Helvetica,Arial,sans-serif;background:#f4f7f5;")
          .append("margin:0;padding:24px;color:#1c2e25;'>");

        // Header
        sb.append("<div style='max-width:680px;margin:0 auto;background:#fff;")
          .append("border-radius:12px;border:1px solid #e6ece9;overflow:hidden;'>");
        sb.append("<div style='background:#0f3a26;color:#fff;padding:20px 24px;'>")
          .append("<h1 style='margin:0;font-size:20px;'>")
          .append("2Africa.AI <span style='color:#7BC58A;'>AgriOS</span> &middot; ")
          .append(t.title)
          .append("</h1>")
          .append("<div style='margin-top:4px;font-size:13px;opacity:0.85;'>")
          .append(t.subtitle).append(" (")
          .append(overview.getWindowDays()).append("d)</div>")
          .append("</div>");

        // KPI table -- email clients prefer tables to flex/grid
        sb.append("<div style='padding:18px 24px;'>");
        sb.append("<table role='presentation' style='width:100%;border-collapse:collapse;'>");
        sb.append("<tr>");
        kpiCell(sb, t.kpiTotal,    formatN(overview.getTotalConversations()),    "#0f3a26");
        kpiCell(sb, t.kpiOpen,     formatN(overview.getOpenConversations()),     "#1677ff");
        kpiCell(sb, t.kpiPending,  formatN(overview.getPendingConversations()),  "#b35a00");
        kpiCell(sb, t.kpiResolved, formatN(overview.getResolvedConversations()), "#27774d");
        sb.append("</tr><tr>");
        Long frtAvg = overview.getFrtMetrics() != null ? overview.getFrtMetrics().getAvgSec() : null;
        Long ttrAvg = overview.getTtrMetrics() != null ? overview.getTtrMetrics().getAvgSec() : null;
        Double csatAvg = overview.getCsatMetrics() != null ? overview.getCsatMetrics().getAvgRating() : null;
        kpiCell(sb, t.kpiFrt,  formatDuration(frtAvg), "#7a3e8f");
        kpiCell(sb, t.kpiTtr,  formatDuration(ttrAvg), "#c45a4d");
        kpiCell(sb, t.kpiCsat, csatAvg == null ? "&mdash;" : String.format("%.1f / 5", csatAvg), "#f5b400");
        kpiCellEmpty(sb);    // pad to 4 cols
        sb.append("</tr></table>");

        // Agent leaderboard top 5
        sb.append("<h2 style='font-size:15px;color:#0f3a26;margin:22px 0 8px;'>")
          .append(t.leaderboard).append("</h2>");
        sb.append("<table style='width:100%;border-collapse:collapse;font-size:13px;'>");
        sb.append("<thead><tr style='background:#f4f7f5;text-align:left;'>")
          .append("<th style='padding:8px 10px;'>").append(t.lbAgent).append("</th>")
          .append("<th style='padding:8px 10px;text-align:right;'>").append(t.lbAssigned).append("</th>")
          .append("<th style='padding:8px 10px;text-align:right;'>").append(t.lbResolved).append("</th>")
          .append("<th style='padding:8px 10px;text-align:right;'>").append(t.lbFrt).append("</th>")
          .append("<th style='padding:8px 10px;text-align:right;'>").append(t.lbTtr).append("</th>")
          .append("</tr></thead><tbody>");
        if (leaderboard.getRows() == null || leaderboard.getRows().isEmpty()) {
            sb.append("<tr><td colspan='5' style='padding:14px;text-align:center;color:#8a9690;'>")
              .append(t.empty).append("</td></tr>");
        } else {
            int n = Math.min(5, leaderboard.getRows().size());
            for (int i = 0; i < n; i++) {
                AgentLeaderboardVO.AgentRow r = leaderboard.getRows().get(i);
                sb.append("<tr style='border-top:1px solid #eef2f0;'>")
                  .append("<td style='padding:8px 10px;'>").append(escape(r.getAgentName())).append("</td>")
                  .append("<td style='padding:8px 10px;text-align:right;'>").append(formatN(r.getAssignedCount())).append("</td>")
                  .append("<td style='padding:8px 10px;text-align:right;'>").append(formatN(r.getResolvedCount())).append("</td>")
                  .append("<td style='padding:8px 10px;text-align:right;'>").append(formatDuration(r.getFrtAvgSec())).append("</td>")
                  .append("<td style='padding:8px 10px;text-align:right;'>").append(formatDuration(r.getTtrAvgSec())).append("</td>")
                  .append("</tr>");
            }
        }
        sb.append("</tbody></table>");

        // Footer
        sb.append("<p style='margin:22px 0 0;font-size:11px;color:#8a9690;'>")
          .append(t.footnote).append("</p>");
        sb.append("</div>");    // padding wrapper
        sb.append("</div>");    // card
        sb.append("</body></html>");
        return sb.toString();
    }

    private static void kpiCell(StringBuilder sb, String label, String value, String color) {
        sb.append("<td style='width:25%;padding:10px 8px;vertical-align:top;'>")
          .append("<div style='background:#fff;border:1px solid #e6ece9;border-top:3px solid ")
          .append(color)
          .append(";border-radius:8px;padding:10px 12px;'>")
          .append("<div style='font-size:11px;color:#5b6b62;text-transform:uppercase;letter-spacing:0.4px;font-weight:600;'>")
          .append(label).append("</div>")
          .append("<div style='font-size:22px;font-weight:700;color:#0f3a26;margin-top:2px;'>")
          .append(value).append("</div>")
          .append("</div></td>");
    }

    private static void kpiCellEmpty(StringBuilder sb) {
        sb.append("<td style='width:25%;'></td>");
    }

    private static String formatN(Integer n) {
        return n == null ? "&mdash;" : String.format("%,d", n);
    }

    private static String formatDuration(Long sec) {
        if (sec == null || sec < 0) return "&mdash;";
        if (sec < 60)    return sec + "s";
        if (sec < 3600)  return Math.round(sec / 60.0) + "m";
        if (sec < 86400) {
            long h = sec / 3600;
            long m = Math.round((sec % 3600) / 60.0);
            return m > 0 ? h + "h " + m + "m" : h + "h";
        }
        long d = sec / 86400;
        long h = Math.round((sec % 86400) / 3600.0);
        return h > 0 ? d + "d " + h + "h" : d + "d";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /**
     * Minimal locale-bundle. Kept inline (rather than via a real
     * MessageSource) because the digest is the only consumer and the
     * key list is short -- two files of overhead for no win.
     */
    private record I18n(
            String title, String subtitle,
            String kpiTotal, String kpiOpen, String kpiPending, String kpiResolved,
            String kpiFrt, String kpiTtr, String kpiCsat,
            String leaderboard, String lbAgent, String lbAssigned,
            String lbResolved, String lbFrt, String lbTtr,
            String empty, String footnote
    ) {
        static I18n forLocale(Locale loc) {
            String lang = loc == null ? "en" : loc.getLanguage();
            return switch (lang) {
                case "zh" -> new I18n(
                        "客服周报", "过去 7 天客户服务总览",
                        "总会话", "进行中", "待处理", "已解决",
                        "平均首响应", "平均解决时长", "平均 CSAT",
                        "Agent 排行榜", "Agent", "分配数", "已解决",
                        "平均 FRT", "平均 TTR",
                        "本周窗口内暂无 agent 数据",
                        "数据来自 Chatwoot + AgriOS CS-Core，详情请登录 dashboard 查看完整图表。"
                );
                case "sw" -> new I18n(
                        "Ripoti ya Wiki ya Wateja", "Muhtasari wa siku 7 zilizopita",
                        "Jumla", "Wazi", "Yanasubiri", "Yamesuluhishwa",
                        "Wastani wa jibu la kwanza", "Wastani wa kusuluhisha", "Wastani wa CSAT",
                        "Ubao wa wakala", "Wakala", "Imegawiwa", "Imesuluhishwa",
                        "Wastani FRT", "Wastani TTR",
                        "Hakuna shughuli za wakala katika dirisha hili",
                        "Data inatoka Chatwoot + AgriOS CS-Core; ingia dashboard kwa picha kamili."
                );
                default -> new I18n(
                        "Customer Service Weekly", "Last 7 days at a glance",
                        "Total", "Open", "Pending", "Resolved",
                        "Avg first response", "Avg time to resolution", "Avg CSAT",
                        "Agent leaderboard", "Agent", "Assigned", "Resolved",
                        "Avg FRT", "Avg TTR",
                        "No agent activity in window",
                        "Data sourced from Chatwoot + AgriOS CS-Core. Open the dashboard for the full charts."
                );
            };
        }
    }
}
