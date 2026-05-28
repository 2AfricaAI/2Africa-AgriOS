package ai.toafrica.agrios.qc.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * GAP / HACCP audit report data for a single batch (Sprint 28).
 *
 * Composes the existing trace data (Sprint 25) plus compliance verdicts that
 * matter to GlobalG.A.P / KEPHIS auditors:
 *   - PHI compliance for every spray activity used on this batch
 *   - QC inspections summary (incoming / in-process / outgoing)
 *   - Any complaints or recall events tied to the batch (food-safety audit trail)
 */
@Data
public class GapReportData {

    /** Embedded full traceability chain (reuses TraceVO from Sprint 25). */
    private TraceVO trace;

    /** Computed PHI compliance verdict for the harvest. */
    private PhiCompliance phiCompliance;

    /** QC inspection summary. */
    private QcSummary qcSummary;

    /** Complaints opened against this batch (if any). */
    private List<ComplaintEntry> complaints;

    /** Recalls of this batch (usually 0 or 1). */
    private List<RecallEntry> recalls;

    /** Report generation context. */
    private LocalDateTime generatedAt;
    private String generatedByName;

    /**
     * Overall verdict — derived from the above:
     *   COMPLIANT  — PHI ok + QC all pass + no open recall
     *   FLAGGED    — PHI ok + QC has fail / non-fatal complaint
     *   NON_COMPLIANT — PHI breach or active recall
     */
    private String verdict;

    @Data
    public static class PhiCompliance {
        /** true if every spray activity respected its PHI before harvest. */
        private boolean compliant;
        /** Earliest safe harvest date implied by the latest spray (null if no sprays). */
        private LocalDate earliestSafeDate;
        /** Actual harvest date. */
        private LocalDate harvestDate;
        /** Per-spray detail (only sprays, fertilize/water excluded). */
        private List<SprayCheck> sprayChecks;
    }

    @Data
    public static class SprayCheck {
        private LocalDate sprayDate;
        private String inputCode;
        private String inputName;
        private String activeIngredient;
        private Integer phiDays;
        private LocalDate safeDate;        // sprayDate + phiDays
        private LocalDate harvestDate;
        private boolean compliant;         // harvestDate >= safeDate
    }

    @Data
    public static class QcSummary {
        private Integer totalInspections;
        private Integer pass;
        private Integer fail;
        private Integer conditionalPass;
        private Integer pending;
        private List<InspectionEntry> inspections;
    }

    @Data
    public static class InspectionEntry {
        private Long id;
        private String code;
        private String inspectionType;     // incoming / in_process / outgoing
        private LocalDate inspectDate;
        private String inspectorName;
        private String result;
        private String resultRemark;
    }

    @Data
    public static class ComplaintEntry {
        private Long id;
        private String code;
        private LocalDateTime reportedAt;
        private String category;
        private String severity;
        private String status;
        private String customerName;
        private String description;
    }

    @Data
    public static class RecallEntry {
        private Long id;
        private String code;
        private LocalDateTime triggeredAt;
        private String status;
        private String reason;
        private Integer affectedOrderCount;
    }
}
