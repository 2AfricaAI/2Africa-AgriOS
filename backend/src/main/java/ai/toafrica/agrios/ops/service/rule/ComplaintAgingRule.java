package ai.toafrica.agrios.ops.service.rule;

import ai.toafrica.agrios.ops.entity.ActionItem;
import ai.toafrica.agrios.qc.entity.Complaint;
import ai.toafrica.agrios.qc.mapper.ComplaintMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * R-COMP-01 — Complaint aging (Sprint 27).
 *
 * Trigger: complaint.status = 'open' AND reported_at older than 48 hours.
 *   tab        = today
 *   severity   = high (critical complaints upgrade to urgent)
 *   owner_role = sales
 *   ref        = complaint.id
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComplaintAgingRule implements ActionRule {

    private static final long STALE_HOURS = 48L;

    private final ComplaintMapper complaintMapper;

    @Override public String ruleCode()  { return "R-COMP-01"; }
    @Override public String category()  { return "today"; }
    @Override public String severity()  { return "high"; }
    @Override public String ownerRole() { return "sales"; }

    @Override
    public List<ActionItem> evaluate() {
        LocalDateTime cutoff = LocalDateTime.now().minus(STALE_HOURS, ChronoUnit.HOURS);

        List<Complaint> rows = complaintMapper.selectList(
                new LambdaQueryWrapper<Complaint>()
                        .eq(Complaint::getStatus, "open")
                        .lt(Complaint::getReportedAt, cutoff));

        List<ActionItem> out = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (Complaint c : rows) {
            long hours = ChronoUnit.HOURS.between(c.getReportedAt(), LocalDateTime.now());
            String sev = "critical".equals(c.getSeverity()) ? "urgent" : severity();

            ActionItem a = new ActionItem();
            a.setRuleCode(ruleCode());
            a.setCategory(category());
            a.setSeverity(sev);
            a.setOwnerRole(ownerRole());
            a.setTitle(String.format("Unresolved complaint — %s (%dh, %s/%s)",
                    c.getCode(), hours, c.getCategory(), c.getSeverity()));
            a.setDescription(String.format(
                    "Reported %s. Severity %s. Still open after %dh. Move it to investigating or resolve.",
                    c.getReportedAt(), c.getSeverity(), hours));
            a.setRefType("complaint");
            a.setRefId(c.getId());
            a.setRefCode(c.getCode());
            a.setDueDate(today);
            a.setDataSnapshot(String.format(
                    "{\"complaint_id\":%d,\"severity\":\"%s\",\"hours_open\":%d}",
                    c.getId(), c.getSeverity(), hours));
            out.add(a);
        }
        if (!out.isEmpty()) {
            log.info("[Rule R-COMP-01] aging complaints triggered = {}", out.size());
        }
        return out;
    }
}
