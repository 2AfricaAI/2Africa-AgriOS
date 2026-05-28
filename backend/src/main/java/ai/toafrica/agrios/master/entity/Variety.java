package ai.toafrica.agrios.master.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("variety")
public class Variety {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long cropId;
    private String code;
    private String name;
    private String traits;

    /** Override of crop.shelf_life_days; null = use crop default. */
    private Integer shelfLifeDays;

    /** 1=启用 0=停用 */
    private Integer status;

    private LocalDateTime createdAt;
}
