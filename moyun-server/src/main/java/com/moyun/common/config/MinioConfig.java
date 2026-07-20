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
     * 访问地址（MinIO 服务内部地址，用于初始化 MinioClient）
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

    /**
     * v1.1.2 新增：对外访问 URL 前缀。
     * 生产环境 MinIO 通常部署在内网，外网用户无法访问 endpoint。
     * 此字段用于拼接返回给前端的 fileUrl，若为空则回退到 endpoint。
     * 配置示例：https://cdn.example.com
     */
    private String accessUrl;

    @Bean
    public MinioClient minioClient() {
        // v1.1.2 修复：MinIO 8.5.x 的 Builder 只有 timeout(long, TimeUnit) 重载，
        // 不支持 timeout(Duration) 单参形式，原代码编译失败。直接移除自定义超时，
        // 使用 MinIO 默认超时配置（10s connect / 10s write / 10s read）。
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    /**
     * v1.1.2 新增：获取对外访问 URL 前缀。
     * 优先用 accessUrl（生产环境应配置 CDN 或外网域名），为空时回退到 endpoint。
     */
    public String getAccessUrlOrEndpoint() {
        return (accessUrl != null && !accessUrl.isEmpty()) ? accessUrl : endpoint;
    }
}
