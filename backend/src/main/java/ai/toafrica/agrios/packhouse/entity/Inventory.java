package ai.toafrica.agrios.packhouse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("inventory")
public class Inventory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long skuId;
    private Long batchId;
    private String grade;
    private Long locationId;
    private BigDecimal qtyAvail;
    private BigDecimal qtyLocked;
    private BigDecimal qtyInTransit;
    private String unit;
    private LocalDate prodDate;
    /** Best-before date = pack_date + shelf_life (FEFO). Null = no expiry tracking. */
    private LocalDate expiryDate;
    private String status;
    @Version private Integer version;
    private LocalDateTime lastOpAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
