package com.albertsfarm.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 对象存储配置 (MinIO / 阿里云 OSS 通用)
 *
 * <p>endpoint 与 publicEndpoint 的区别:</p>
 * <ul>
 *   <li>endpoint        - 后端到对象存储的内部地址 (容器网络/VPC),用于上传下载</li>
 *   <li>publicEndpoint  - 浏览器/客户端访问的公开地址,用于生成预签名 URL</li>
 * </ul>
 * 生产 OSS 上两者通常相同,此时只设置 endpoint 即可(publicEndpoint 会自动回落)。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "albertsfarm.oss")
public class OssProperties {
    private String endpoint;
    private String publicEndpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;

    /** 公开访问地址 - 没显式配则回落到 endpoint */
    public String resolvedPublicEndpoint() {
        return (publicEndpoint == null || publicEndpoint.isBlank()) ? endpoint : publicEndpoint;
    }
}
