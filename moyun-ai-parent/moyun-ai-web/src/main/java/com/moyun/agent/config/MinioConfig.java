package com.moyun.agent.config;

import io.minio.MinioClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 对象存储配置
 *
 * @author laomao
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {

    /**
     * MinIO 服务端点
     */
    private String endpoint;

    /**
     * 访问密钥
     */
    private String accessKey;

    /**
     * 秘密密钥
     */
    private String secretKey;

    /**
     * 存储桶配置
     */
    private BucketConfig bucket = new BucketConfig();

    @Data
    public static class BucketConfig {
        /**
         * 知识库文件存储桶
         */
        private String knowledge = "knowledge-files";

        /**
         * 图片存储桶
         */
        private String images = "knowledge-images";
    }

    @Bean
    public MinioClient minioClient() {
        log.info("✅ 初始化 MinIO 客户端: {}", endpoint);
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
