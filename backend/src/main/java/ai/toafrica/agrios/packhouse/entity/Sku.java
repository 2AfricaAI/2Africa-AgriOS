package ai.toafrica.agrios.packhouse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sku")
public class Sku {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private Long cropId;
    private Long varietyId;
    /** A / B / C */
    private String grade;
    /** packaging_spec.id */
    private Long specId;
    private String unit;
    private Integer status;
    private LocalDateTime createdAt;
}
