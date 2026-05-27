package ai.toafrica.agrios.master.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InputItemVO {
    private Long id;
    private String code;
    private String name;
    private String nameEn;
    private String inputType;
    private String spec;
    private String unit;
    private String activeIngredient;
    private String registrationNo;
    private Integer phiDays;
    private Long defaultSupplierId;
    private String defaultSupplierName;  // populated by service via supplier mapper
    private String status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
