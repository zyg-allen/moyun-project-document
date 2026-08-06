package com.moyun.ext.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI 模块对象存储路径配置
 *
 * <p>读取 moyun-ai.storage 配置，用于知识库文档和图片在 MinIO 中的存储路径与桶名。
 * 复用 moyun-server 主项目的 MinioClient Bean（由 com.moyun.common.config.MinioConfig 提供），
 * 此类仅提供 AI 模块专用的存储路径配置。</p>
 *
 * <p>原 com.moyun.ext.ai.config.MinioConfig 因与 com.moyun.common.config.MinioConfig 类名冲突
 * （Bean 名重复），已重命名为 AiStorageConfig 以消除冲突。</p>
 *
 * @author laomao
 */
@Data
@Configuration("aiStorageConfig")
@ConfigurationProperties(prefix = "moyun-ai.storage")
public class AiStorageConfig {

    /** 知识库文件存储目录 */
    private String knowledgeBaseDir = "/profile/ai/knowledge";

    /** 图片存储目录 */
    private String imageDir = "/profile/ai/images";

    /** 存储桶配置 */
    private Bucket bucket = new Bucket();

    @Data
    public static class Bucket {
        /** 知识库存储桶名 */
        private String knowledge = "moyun-ai-knowledge";
        /** 图片存储桶名 */
        private String images = "moyun-ai-images";
    }
}
