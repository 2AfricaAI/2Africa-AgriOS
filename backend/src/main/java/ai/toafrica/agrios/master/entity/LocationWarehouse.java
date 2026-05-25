package ai.toafrica.agrios.master.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("location_warehouse")
public class LocationWarehouse {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;
    private String name;

    /** normal / cold / quarantine */
    private String type;

    /** 0=顶层节点 */
    private Long parentId;

    private BigDecimal capacityKg;

    /** 1=启用 0=停用 */
    private Integer status;

    private LocalDateTime createdAt;
}
