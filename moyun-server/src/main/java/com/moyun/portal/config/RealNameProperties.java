package com.moyun.portal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 实名认证通道配置（银行卡四要素核验等场景共用）
 *
 * <p>配置来源：application.yaml 的 moyun.realname 段。生产环境 AccessKey 等
 * 敏感参数务必通过环境变量注入（如 MOYUN_REALNAME_ALIYUN_ACCESSKEYID）。
 *
 * <p>enabled=false 或 AccessKey 未配置/为 todo 占位值时，四要素核验明确
 * 返回"通道未开通"，调用方按降级处理（不伪装核验通过）。
 *
 * @author moyun
 */
@Component
@ConfigurationProperties(prefix = "moyun.realname")
public class RealNameProperties {

    /** 是否启用第三方实名核验（false=通道未开通，核验明确返回未开通） */
    private boolean enabled = false;

    /** 通道提供方：aliyun / tencent */
    private String provider = "aliyun";

    /** 同一用户四要素核验每日上限（防重复调用产生通道费用） */
    private int dailyVerifyLimit = 5;

    /** 阿里云实名认证配置 */
    private Aliyun aliyun = new Aliyun();

    public static class Aliyun {
        /** AccessKey ID（生产走环境变量注入） */
        private String accessKeyId;
        /** AccessKey Secret（生产走环境变量注入） */
        private String accessKeySecret;
        /** API 端点（实名认证/银行卡四要素核验） */
        private String endpoint = "cloudauth.aliyuncs.com";

        public String getAccessKeyId() { return accessKeyId; }
        public void setAccessKeyId(String accessKeyId) { this.accessKeyId = accessKeyId; }
        public String getAccessKeySecret() { return accessKeySecret; }
        public void setAccessKeySecret(String accessKeySecret) { this.accessKeySecret = accessKeySecret; }
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public int getDailyVerifyLimit() { return dailyVerifyLimit; }
    public void setDailyVerifyLimit(int dailyVerifyLimit) { this.dailyVerifyLimit = dailyVerifyLimit; }
    public Aliyun getAliyun() { return aliyun; }
    public void setAliyun(Aliyun aliyun) { this.aliyun = aliyun; }
}
