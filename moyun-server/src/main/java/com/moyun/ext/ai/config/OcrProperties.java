package com.moyun.ext.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OCR 识别服务配置
 *
 * <p>配置来源：application.yaml 的 moyun.ocr 段。生产环境 AccessKey 等
 * 敏感参数务必通过环境变量注入（如 MOYUN_OCR_ALIYUN_ACCESSKEYID）。
 *
 * <p>enabled=false 或 AccessKey 未配置/为 todo 占位值时，识别接口走
 * 降级路径（返回 success=false，前端回退手动填写表单）。
 *
 * @author moyun
 */
@Data
@Component
@ConfigurationProperties(prefix = "moyun.ocr")
public class OcrProperties {

    /** 是否启用 OCR 识别（false=降级：直接返回未配置，前端手动填写） */
    private boolean enabled = false;

    /** 服务提供方：aliyun / tencent / baidu / paddle（当前骨架按 aliyun 预留） */
    private String provider = "aliyun";

    /** 阿里云 OCR 配置 */
    private Aliyun aliyun = new Aliyun();

    public static class Aliyun {
        /** AccessKey ID（生产走环境变量注入） */
        private String accessKeyId;
        /** AccessKey Secret（生产走环境变量注入） */
        private String accessKeySecret;
        /** API 端点 */
        private String endpoint = "ocr-api.cn-hangzhou.aliyuncs.com";

        public String getAccessKeyId() { return accessKeyId; }
        public void setAccessKeyId(String accessKeyId) { this.accessKeyId = accessKeyId; }
        public String getAccessKeySecret() { return accessKeySecret; }
        public void setAccessKeySecret(String accessKeySecret) { this.accessKeySecret = accessKeySecret; }
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    }
}
