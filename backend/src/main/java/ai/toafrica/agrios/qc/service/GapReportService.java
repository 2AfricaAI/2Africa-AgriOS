package ai.toafrica.agrios.qc.service;

import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.framework.security.SecurityUtil;
import ai.toafrica.agrios.qc.entity.Complaint;
import ai.toafrica.agrios.qc.entity.QcInspection;
import ai.toafrica.agrios.qc.entity.Recall;
import ai.toafrica.agrios.qc.mapper.ComplaintMapper;
import ai.toafrica.agrios.qc.mapper.QcInspectionMapper;
import ai.toafrica.agrios.qc.mapper.RecallMapper;
import ai.toafrica.agrios.qc.vo.GapReportData;
import ai.toafrica.agrios.qc.vo.TraceVO;
import ai.toafrica.agrios.system.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * GAP / HACCP report data aggregation (Sprint 28).
 *
 * Single entry point: {@link #buildBatchReport(String)} returns the full
 * {@link GapReportData} structure for a given batch code; PDF and Excel
 * renderers consume it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GapReportService {

    private final BatchTraceService traceService;
    private final QcInspectionMapper qcMapper;
    private final ComplaintMapper complaintMapper;
    private final RecallMapper recallMapper;
    private final SysUserMapper sysUserMapper;

    // ------------------------------------------------------------
    // Public — build full report data for a batch
    // ------------------------------------------------------------
    public GapReportData buildBatchReport(String batchCode) {
        TraceVO trace = traceService.trace(batchCode);
        if (trace == null || trace.getBatch() == null) {
            throw new BusinessException(R.NOT_FOUND, "Batch not found: " + batchCode);
        }

        GapReportData out = new GapReportData();
        out.setTrace(trace);
        out.setGeneratedAt(LocalDateTime.now());
        Long uid = SecurityUtil.currentUserId();
        if (uid != null) {
            var u = sysUserMapper.selectById(uid);
            if (u != null) out.setGeneratedByName(u.getNickname() != null ? u.getNickname() : u.getUsername());
        }

        // 1) PHI compliance
        out.setPhiCompliance(computePhi(trace));

        // 2) QC summary — inspections directly attached to the batch
        out.setQcSummary(computeQc(trace.getBatch().getId()));

        // 3) Complaints
        out.setComplaints(loadComplaints(trace.getBatch().getId()));

        // 4) Recalls
        out.setRecalls(loadRecalls(trace.getBatch().getId()));

        // 5) Overall verdict
        out.setVerdict(deriveVerdict(out));
        return out;
    }

    // ------------------------------------------------------------
    // PHI compliance — re-derived from trace activities
    // ------------------------------------------------------------
    private GapReportData.PhiCompliance computePhi(TraceVO trace) {
        GapReportData.PhiCompliance phi = new GapReportData.PhiCompliance();
        LocalDate harvest = trace.getHarvest() == null ? null : trace.getHarvest().getHarvestDate();
        phi.setHarvestDate(harvest);

        List<GapReportData.SprayCheck> checks = new ArrayList<>();
        LocalDate latestSafe = null;
        boolean allCompliant = true;
        if (trace.getActivities() != null) {
            for (TraceVO.ActivityNode a : trace.getActivities()) {
                if (!"spray".equalsIgnoreCase(a.getActivityType())) continue;
                if (a.getInputs() == null) continue;
                for (TraceVO.InputUsed iu : a.getInputs()) {
                    if (iu.getPhiDays() == null || iu.getPhiDays() <= 0) continue;
                    LocalDate safe = a.getOccurDate().plusDays(iu.getPhiDays());
                    GapReportData.SprayCheck c = new GapReportData.SprayCheck();
                    c.setSprayDate(a.getOccurDate());
                    c.setInputCode(iu.getInputItemCode());
                    c.setInputName(iu.getInputItemName());
                    c.setActiveIngredient(iu.getActiveIngredient());
                    c.setPhiDays(iu.getPhiDays());
                    c.setSafeDate(safe);
                    c.setHarvestDate(harvest);
                    c.setCompliant(harvest != null && !harvest.isBefore(safe));
                    checks.add(c);
                    if (!c.isCompliant()) allCompliant = false;
                    if (latestSafe == null || safe.isAfter(latestSafe)) latestSafe = safe;
                }
            }
        }
        phi.setSprayChecks(checks);
        phi.setEarliestSafeDate(latestSafe);
        phi.setCompliant(allCompliant);
        return phi;
    }

    // ------------------------------------------------------------
    // QC summary — inspections where ref_type=batch / packing for this batch
    // ------------------------------------------------------------
    private GapReportData.QcSummary computeQc(Long batchId) {
        // Inspections linked directly to the batch
        List<QcInspection> rows = qcMapper.selectList(
                new LambdaQueryWrapper<QcInspection>()
                        .eq(QcInspection::getRefType, "batch")
                        .eq(QcInspection::getRefId, batchId));

        GapReportData.QcSummary s = new GapReportData.QcSummary();
        s.setTotalInspections(rows.size());
        int pass = 0, fail = 0, cond = 0, pend = 0;
        List<GapReportData.InspectionEntry> entries = new ArrayList<>();
        for (QcInspection q : rows) {
            switch (q.getResult() == null ? "pending" : q.getResult()) {
                case "pass" -> pass++;
                case "fail" -> fail++;
                case "conditional_pass" -> cond++;
                default -> pend++;
            }
            GapReportData.InspectionEntry e = new GapReportData.InspectionEntry();
            e.setId(q.getId());
            e.setCode(q.getCode());
            e.setInspectionType(q.getInspectionType());
            e.setInspectDate(q.getInspectDate());
            e.setResult(q.getResult());
            e.setResultRemark(q.getResultRemark());
            if (q.getInspectorId() != null) {
                var u = sysUserMapper.selectById(q.getInspectorId());
                if (u != null) e.setInspectorName(u.getNickname() != null ? u.getNickname() : u.getUsername());
            }
            entries.add(e);
        }
        s.setPass(pass);
        s.setFail(fail);
        s.setConditionalPass(cond);
        s.setPending(pend);
        s.setInspections(entries);
        return s;
    }

    // ------------------------------------------------------------
    // Complaints for the batch
    // ------------------------------------------------------------
    private List<GapReportData.ComplaintEntry> loadComplaints(Long batchId) {
        List<Complaint> rows = complaintMapper.selectList(
                new LambdaQueryWrapper<Complaint>()
                        .eq(Complaint::getBatchId, batchId)
                        .orderByDesc(Complaint::getReportedAt));
        List<GapReportData.ComplaintEntry> out = new ArrayList<>();
        for (Complaint c : rows) {
            GapReportData.ComplaintEntry e = new GapReportData.ComplaintEntry();
            e.setId(c.getId());
            e.setCode(c.getCode());
            e.setReportedAt(c.getReportedAt());
            e.setCategory(c.getCategory());
            e.setSeverity(c.getSeverity());
            e.setStatus(c.getStatus());
            e.setDescription(c.getDescription());
            out.add(e);
        }
        return out;
    }

    // ------------------------------------------------------------
    // Recalls for the batch
    // ------------------------------------------------------------
    private List<GapReportData.RecallEntry> loadRecalls(Long batchId) {
        List<Recall> rows = recallMapper.selectList(
                new LambdaQueryWrapper<Recall>()
                        .eq(Recall::getBatchId, batchId)
                        .orderByDesc(Recall::getTriggeredAt));
        List<GapReportData.RecallEntry> out = new ArrayList<>();
        for (Recall r : rows) {
            GapReportData.RecallEntry e = new GapReportData.RecallEntry();
            e.setId(r.getId());
            e.setCode(r.getCode());
            e.setTriggeredAt(r.getTriggeredAt());
            e.setStatus(r.getStatus());
            e.setReason(r.getReason());
            e.setAffectedOrderCount(r.getAffectedOrderCount());
            out.add(e);
        }
        return out;
    }

    // ------------------------------------------------------------
    // Verdict
    // ------------------------------------------------------------
    private String deriveVerdict(GapReportData d) {
        boolean hasActiveRecall = d.getRecalls() != null && d.getRecalls().stream()
                .anyMatch(r -> !"closed".equals(r.getStatus()));
        boolean phiOk = d.getPhiCompliance() == null || d.getPhiCompliance().isCompliant();
        boolean qcOk = d.getQcSummary() == null || d.getQcSummary().getFail() == 0;
        boolean noOpenComplaints = d.getComplaints() == null || d.getComplaints().stream()
                .allMatch(c -> "closed".equals(c.getStatus()) || "resolved".equals(c.getStatus()));

        if (!phiOk || hasActiveRecall) return "NON_COMPLIANT";
        if (!qcOk || !noOpenComplaints) return "FLAGGED";
        return "COMPLIANT";
    }
}
