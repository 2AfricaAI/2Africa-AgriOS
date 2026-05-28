package ai.toafrica.agrios.qc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "QC inspection — create / submit form")
public class QcInspectionForm {

    @NotBlank
    @Pattern(regexp = "incoming|in_process|outgoing")
    private String inspectionType;

    @Size(max = 32)
    private String refType;
    private Long refId;
    @Size(max = 64)
    private String refCode;

    @NotNull
    private LocalDate inspectDate;

    @Pattern(regexp = "pending|pass|conditional_pass|fail")
    private String result;

    @Size(max = 500)
    private String resultRemark;

    private List<Long> photoIds;

    @Size(max = 500)
    private String remark;

    private List<Item> items;

    @Data
    public static class Item {
        @NotBlank
        @Size(max = 64)
        private String checkPoint;
        @Size(max = 128)
        private String expectedValue;
        @Size(max = 128)
        private String actualValue;
        @Pattern(regexp = "pass|fail|pending|^$")
        private String result;
        @Size(max = 255)
        private String remark;
    }
}
