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
    private String remark;

    /** 1=启用 0=停用 */
    private Integer status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
