package ai.toafrica.agrios.system.service;

import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.framework.config.OssProperties;
import ai.toafrica.agrios.framework.config.UploadProperties;
import ai.toafrica.agrios.framework.security.SecurityUtil;
import ai.toafrica.agrios.system.entity.SysFile;
import ai.toafrica.agrios.system.mapper.SysFileMapper;
import ai.toafrica.agrios.system.vo.FileVO;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    // Spring 按字段名匹配同类型 bean: minioClient -> 内部 endpoint, minioPresignClient -> 公开 endpoint
    private final MinioClient minioClient;          // 上传/删除
    private final MinioClient minioPresignClient;   // 生成预签名 URL
    private final OssProperties oss;
    private final UploadProperties uploadCfg;
    private final SysFileMapper fileMapper;

    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy/MM");

    /**
     * 上传文件到对象存储,并落表
     *
     * @param file    multipart 文件
     * @param bizType 业务分类, 例如 avatar / crop_image, null 则归入 misc
     */
    public FileVO upload(MultipartFile file, String bizType) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件为空");
        }

        // 1) 大小校验
        long maxBytes = uploadCfg.getMaxSizeMb() * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new BusinessException("文件超过 " + uploadCfg.getMaxSizeMb() + " MB 限制");
        }

        // 2) MIME 白名单
        String mime = file.getContentType();
        List<String> whitelist = uploadCfg.getAllowedMimeTypes();
        if (whitelist != null && !whitelist.isEmpty()
                && (mime == null || !whitelist.contains(mime))) {
            throw new BusinessException("不支持的文件类型: " + mime);
        }

        // 3) 生成对象 key: {bizType}/{yyyy}/{MM}/{uuid}.{ext}
        String safeBizType = (bizType == null || bizType.isBlank()) ? "misc" : bizType.trim();
        String original = file.getOriginalFilename() == null ? "noname" : file.getOriginalFilename();
        String ext = extractExt(original);
        String objectKey = "%s/%s/%s%s".formatted(
                safeBizType,
                LocalDate.now().format(YM),
                UUID.randomUUID().toString().replace("-", ""),
                ext.isEmpty() ? "" : ("." + ext)
        );

        // 4) 上传到 MinIO
        try (var in = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(oss.getBucket())
                    .object(objectKey)
                    .stream(in, file.getSize(), -1)
                    .contentType(mime == null ? "application/octet-stream" : mime)
                    .build());
        } catch (Exception e) {
            log.error("[上传失败] key={}", objectKey, e);
            throw new BusinessException("上传失败: " + e.getMessage());
        }

        // 5) 落表
        SysFile record = new SysFile();
        record.setObjectKey(objectKey);
        record.setOriginalName(original);
        record.setBucket(oss.getBucket());
        record.setSizeBytes(file.getSize());
        record.setMimeType(mime);
        record.setExt(ext);
        record.setBizType(safeBizType);
        record.setUploadedBy(SecurityUtil.currentUserId());
        fileMapper.insert(record);

        log.info("[上传成功] uid={} key={} size={} mime={}",
                record.getUploadedBy(), objectKey, file.getSize(), mime);

        return toVO(record);
    }

    /** 取一个预签名下载 URL */
    public String presignedDownloadUrl(Long fileId) {
        SysFile f = fileMapper.selectById(fileId);
        if (f == null) {
            throw new BusinessException(R.NOT_FOUND, "文件不存在");
        }
        return generatePresignedUrl(f.getObjectKey());
    }

    /** 删除文件 - 软删数据库 + 真删对象存储 */
    public void delete(Long fileId) {
        SysFile f = fileMapper.selectById(fileId);
        if (f == null) {
            return;
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(f.getBucket())
                    .object(f.getObjectKey())
                    .build());
        } catch (Exception e) {
            log.warn("[删除对象失败,忽略] key={} err={}", f.getObjectKey(), e.getMessage());
        }
        fileMapper.deleteById(fileId);  // @TableLogic 软删
    }

    /** 取元数据 (附预签名 URL) */
    public FileVO get(Long fileId) {
        SysFile f = fileMapper.selectById(fileId);
        if (f == null) {
            throw new BusinessException(R.NOT_FOUND, "文件不存在");
        }
        return toVO(f);
    }

    // ============================================================
    // 内部辅助
    // ============================================================

    private FileVO toVO(SysFile f) {
        return FileVO.builder()
                .id(f.getId())
                .objectKey(f.getObjectKey())
                .originalName(f.getOriginalName())
                .mimeType(f.getMimeType())
                .sizeBytes(f.getSizeBytes())
                .bizType(f.getBizType())
                .createdAt(f.getCreatedAt())
                .downloadUrl(generatePresignedUrl(f.getObjectKey()))
                .build();
    }

    private String generatePresignedUrl(String objectKey) {
        try {
            return minioPresignClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(oss.getBucket())
                    .object(objectKey)
                    .expiry(uploadCfg.getPresignedUrlExpireMinutes(), TimeUnit.MINUTES)
                    .build());
        } catch (Exception e) {
            log.error("[生成预签名URL失败] key={}", objectKey, e);
            throw new BusinessException("生成下载链接失败");
        }
    }

    private String extractExt(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return "";
        return filename.substring(dot + 1).toLowerCase();
    }
}
