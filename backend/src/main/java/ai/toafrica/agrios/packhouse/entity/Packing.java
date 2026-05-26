package ai.toafrica.agrios.packhouse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("packing")
public class Packing {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;           // PK-yyyyMMdd-NNN
    private Long batchId;
    private String grade;
    private Long specId;
    private Long skuId;
    private Integer qtyUnits;      // 件数
    private BigDecimal netWeightKg;
    private Long locationId;
    private LocalDateTime packedAt;
    private Long operatorId;
    private String remark;
    private LocalDateTime createdAt;
}
