package com.albertsfarm.master.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("packaging_spec")
public class PackagingSpec {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;
    private String name;
    private BigDecimal unitNetKg;
    private BigDecimal unitGrossKg;
    private String material;

    /** 1=启用 0=停用 */
    private Integer status;

    private LocalDateTime createdAt;
}
