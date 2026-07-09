package com.moyun.common.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 配置类
 *
 * @author moyun
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {

    /**
     * 访问地址
     */
    private String endpoint;

    /**
     * accessKey
     */
    private String accessKey;

    /**
     * secretKey
     */
    private String secretKey;

    /**
     * 默认存储桶名称
     */
    private String bucketName;

    /**
     * 是否启用MinIO（默认false，false时使用本地存储）
     */
    private Boolean enabled = false;

    /**
     * 手动强制降级到本地存储（运行期可调，无需重启）。
     * enabled=true 但本字段=true 时，所有上传绕过 MinIO 直接落本地。
     */
    private Boolean fallbackToLocal = false;

    /**
     * 自动降级开关：MinIO 上传/连接异常时自动切到本地存储（默认 true）。
     * 关闭后 MinIO 异常将直接抛错，不降级。
     */
    private Boolean autoFallback = true;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
