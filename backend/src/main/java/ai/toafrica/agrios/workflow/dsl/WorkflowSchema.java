package ai.toafrica.agrios.workflow.dsl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Sprint 52 -- in-memory representation of {@code wf_definition.schema_json}.
 *
 * <p>Shape (matches the JSON DSL in PRD-HR-ADMIN-LEGAL-WORKFLOW-v0.2 § 3.1):</p>
 * <pre>{@code
 * {
 *   "trigger": { "always": true },
 *   "steps": [
 *     {
 *       "seq": 1,
 *       "type": "approval",
 *       "assignee": { "lookup": "node.manager_id" },
 *       "sla_hours": 48
 *     },
 *     {
 *       "seq": 2,
 *       "type": "approval",
 *       "assignee": { "role": "CFO" },
 *       "condition": "amount > 50000",
 *       "sla_hours": 24
 *     }
 *   ]
 * }
 * }</pre>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowSchema {

    private Map<String, Object> trigger;
    private List<StepSpec> steps;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StepSpec {
        private Integer seq;
        /** 'approval' / 'cc' / 'sign' / 'pay' */
        private String type;
        private AssigneeSpec assignee;
        /** Inclusive precondition; if false the step is skipped. */
        private String condition;
        private Integer slaHours;
        /** Optional fallback assignee when SLA expires. */
        private AssigneeSpec onEscalate;
    }

    /**
     * One of {@link #userId} / {@link #role} / {@link #lookup} should be set.
     * Engine resolves in that priority order.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AssigneeSpec {
        private Long userId;
        private String role;
        /** Expression resolved at runtime, e.g. 'node.manager_id'. */
        private String lookup;
    }
}
