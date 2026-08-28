package com.moyun.core.sms;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 短信服务配置（V11.1）
 *
 * <p>真实 API 采用阿里云短信；生产环境 AccessKey 等敏感参数务必通过
 * 环境变量注入（如 MOYUN_SMS_ALIYUN_ACCESSKEYID），配置结构本期已就位，
 * SDK 接入点见 {@link AliyunSmsSender} 内 TODO 标注。
 *
 * <p>mock 模式（mock-enabled=true）：验证码写入日志（dev.log），全流程
 * （频控/存储/校验/防枚举）与真实通道一致，仅不发真实短信。
 *
 * @author moyun
 */
@Component
@ConfigurationProperties(prefix = "moyun.sms")
public class SmsProperties {

    /** 短信服务总开关（false 时发送接口直接拒绝） */
    private boolean enabled = true;

    /** 模拟模式：true=验证码写日志不发真实短信；false=走阿里云真实通道 */
    private boolean mockEnabled = true;

    /** 验证码有效期（分钟） */
    private int codeExpireMinutes = 5;

    /** 同一手机号两次发送最小间隔（秒，防轰炸） */
    private int sendIntervalSeconds = 60;

    /** 同一手机号每日发送上限 */
    private int dailyLimit = 10;

    /** 校验失败锁定阈值（连续错误 N 次后作废验证码） */
    private int maxVerifyAttempts = 5;

    /** 阿里云短信配置（真实 API 接入位，mock 模式下不使用） */
    private Aliyun aliyun = new Aliyun();

    public static class Aliyun {
        /** AccessKey ID */
        private String accessKeyId;
        /** AccessKey Secret */
        private String accessKeySecret;
        /** 短信签名 */
        private String signName;
        /** 验证码模板 Code */
        private String templateCode;
        /** API 端点（默认公网） */
        private String endpoint = "dysmsapi.aliyuncs.com";

        public String getAccessKeyId() { return accessKeyId; }
        public void setAccessKeyId(String accessKeyId) { this.accessKeyId = accessKeyId; }
        public String getAccessKeySecret() { return accessKeySecret; }
        public void setAccessKeySecret(String accessKeySecret) { this.accessKeySecret = accessKeySecret; }
        public String getSignName() { return signName; }
        public void setSignName(String signName) { this.signName = signName; }
        public String getTemplateCode() { return templateCode; }
        public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isMockEnabled() { return mockEnabled; }
    public void setMockEnabled(boolean mockEnabled) { this.mockEnabled = mockEnabled; }
    public int getCodeExpireMinutes() { return codeExpireMinutes; }
    public void setCodeExpireMinutes(int codeExpireMinutes) { this.codeExpireMinutes = codeExpireMinutes; }
    public int getSendIntervalSeconds() { return sendIntervalSeconds; }
    public void setSendIntervalSeconds(int sendIntervalSeconds) { this.sendIntervalSeconds = sendIntervalSeconds; }
    public int getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(int dailyLimit) { this.dailyLimit = dailyLimit; }
    public int getMaxVerifyAttempts() { return maxVerifyAttempts; }
    public void setMaxVerifyAttempts(int maxVerifyAttempts) { this.maxVerifyAttempts = maxVerifyAttempts; }
    public Aliyun getAliyun() { return aliyun; }
    public void setAliyun(Aliyun aliyun) { this.aliyun = aliyun; }
}
