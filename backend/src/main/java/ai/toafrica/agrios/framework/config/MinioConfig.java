package ai.toafrica.agrios.framework.config;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 客户端配置
 *
 * <p>提供两个 MinioClient bean:</p>
 * <ul>
 *   <li>minioClient        - 走内部 endpoint,用于后端上传/下载/管理</li>
 *   <li>minioPresignClient - 走 publicEndpoint,仅用于生成浏览器可访问的预签名 URL</li>
 * </ul>
 *
 * <p>为什么需要两个: SigV4 预签名把 host 头放进签名,
 * 所以必须用 "客户端最终访问时使用的 host" 来生成 URL,否则验签失败。</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MinioConfig {

    private final OssProperties oss;

    /**
     * 显式设 region 避免 SDK 在 presign 时通过 HTTP 请求 MinIO 的
     * ?location endpoint 自动探测 region。
     * presign client 用的是公开 endpoint (http://localhost:9000),
     * 从后端容器内 localhost 根本访问不到 MinIO,会让 presign 失败。
     */
    private static final String DEFAULT_REGION = "us-east-1";

    @Bean
    public MinioClient minioClient() {
        log.info("[MinIO] internal endpoint = {}", oss.getEndpoint());
        return MinioClient.builder()
                .endpoint(oss.getEndpoint())
                .credentials(oss.getAccessKey(), oss.getSecretKey())
                .region(DEFAULT_REGION)
                .build();
    }

    @Bean
    public MinioClient minioPresignClient() {
        String publicEp = oss.resolvedPublicEndpoint();
        log.info("[MinIO] public endpoint (for presigned URLs) = {}", publicEp);
        return MinioClient.builder()
                .endpoint(publicEp)
                .credentials(oss.getAccessKey(), oss.getSecretKey())
                .region(DEFAULT_REGION)
                .build();
    }
}
