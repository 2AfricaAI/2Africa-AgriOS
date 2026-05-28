package ai.toafrica.agrios.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "File metadata + presigned download URL")
public class FileVO {
    @Schema(description = "File id")
    private Long id;

    @Schema(description = "Object key, e.g. avatar/2026/05/uuid.jpg")
    private String objectKey;

    @Schema(description = "Original filename")
    private String originalName;

    @Schema(description = "MIME type")
    private String mimeType;

    @Schema(description = "File size (bytes)")
    private Long sizeBytes;

    @Schema(description = "Business category")
    private String bizType;

    @Schema(description = "Upload time")
    private LocalDateTime createdAt;

    @Schema(description = "Presigned download URL (short-lived, usually 1 hour)")
    private String downloadUrl;
}
