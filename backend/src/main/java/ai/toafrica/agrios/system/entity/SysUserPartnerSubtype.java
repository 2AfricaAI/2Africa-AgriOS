package ai.toafrica.agrios.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Sprint 37: A partner user may have multiple subtypes (e.g. AGRONOMIST +
 * GAP_AUDITOR for an export consultant).  Subtype codes are an open string
 * but the well-known values are listed on PartnerSubtype constants.
 */
@Data
@TableName("sys_user_partner_subtype")
public class SysUserPartnerSubtype {
    private Long userId;
    private String subtypeCode;
    private LocalDateTime createdAt;
}
