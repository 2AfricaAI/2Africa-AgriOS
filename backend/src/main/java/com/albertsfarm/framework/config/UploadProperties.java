package com.albertsfarm.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;

/**
 * 文件上传业务配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "albertsfarm.upload")
public class UploadProperties {
    /** 存储类型: oss | minio | local */
    private String type = "minio";

    /** 单个文件最大体积(MB),除了 Spring Boot 的 multipart 限制,服务层再校验一次 */
    private int maxSizeMb = 10;

    /** 预签名下载 URL 的过期时间(分钟) */
    private int presignedUrlExpireMinutes = 60;

    /** 允许的 MIME 类型白名单,空则不限制 */
    private List<String> allowedMimeTypes = Collections.emptyList();
}
