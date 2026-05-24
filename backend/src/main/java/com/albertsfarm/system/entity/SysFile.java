package com.albertsfarm.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_file")
public class SysFile {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 对象存储里的 key, 例如 avatar/2026/05/uuid.jpg */
    private String objectKey;

    /** 上传时的原始文件名 */
    private String originalName;

    /** bucket 名 */
    private String bucket;

    private Long sizeBytes;

    private String mimeType;

    private String ext;

    /** 业务分类: avatar / crop_image / activity_photo / ... */
    private String bizType;

    /** 上传者用户 ID */
    private Long uploadedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic(value = "NULL", delval = "NOW()")
    private LocalDateTime deletedAt;
}
