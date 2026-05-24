package com.albertsfarm.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "文件元数据 + 预签名下载 URL")
public class FileVO {
    @Schema(description = "文件 ID")
    private Long id;

    @Schema(description = "对象 key, 例如 avatar/2026/05/uuid.jpg")
    private String objectKey;

    @Schema(description = "原始文件名")
    private String originalName;

    @Schema(description = "MIME 类型")
    private String mimeType;

    @Schema(description = "文件大小(字节)")
    private Long sizeBytes;

    @Schema(description = "业务分类")
    private String bizType;

    @Schema(description = "上传时间")
    private LocalDateTime createdAt;

    @Schema(description = "预签名下载 URL (短期有效,通常 1 小时)")
    private String downloadUrl;
}
