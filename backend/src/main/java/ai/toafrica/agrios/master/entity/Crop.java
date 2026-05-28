package ai.toafrica.agrios.master.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("crop")
public class Crop {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;
    private String name;
    private String category;
    private String unit;
    private Integer cycleDays;

    /** Default shelf life (days) after packing - FEFO. Variety may override. */
    private Integer shelfLifeDays;

    private String remark;

    /** 1=enabled, 0=disabled */
    private Integer status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
