package ai.toafrica.agrios.workflow.dsl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sprint 52 -- thin wrapper around Jackson with snake_case naming so the
 * DSL can be written as {@code sla_hours} rather than {@code slaHours}.
 */
@Slf4j
@Component
public class WorkflowSchemaParser {

    private final ObjectMapper json = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    public WorkflowSchema parse(String schemaJson) {
        if (schemaJson == null || schemaJson.isBlank()) {
            throw new IllegalArgumentException("schema_json is empty");
        }
        try {
            WorkflowSchema s = json.readValue(schemaJson, WorkflowSchema.class);
            validate(s);
            return s;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid workflow schema JSON: " + e.getMessage(), e);
        }
    }

    private void validate(WorkflowSchema s) {
        List<WorkflowSchema.StepSpec> steps = s.getSteps();
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("Workflow schema must have at least 1 step");
        }
        for (int i = 0; i < steps.size(); i++) {
            WorkflowSchema.StepSpec step = steps.get(i);
            if (step.getSeq() == null) {
                throw new IllegalArgumentException("Step " + i + " is missing 'seq'");
            }
            if (step.getType() == null) step.setType("approval");
            if (step.getAssignee() == null) {
                throw new IllegalArgumentException("Step " + step.getSeq() + " is missing 'assignee'");
            }
            WorkflowSchema.AssigneeSpec a = step.getAssignee();
            if (a.getUserId() == null && (a.getRole() == null || a.getRole().isBlank())
                    && (a.getLookup() == null || a.getLookup().isBlank())) {
                throw new IllegalArgumentException(
                        "Step " + step.getSeq() + " assignee must have one of user_id / role / lookup");
            }
        }
    }
}
