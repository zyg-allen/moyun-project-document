package com.moyun.pay.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 公共支付通道配置（V11.0）
 *
 * <p>配置来源：application.yaml 的 moyun.pay 段。生产环境商户密钥等敏感参数
 * 建议通过环境变量注入（如 MOYUN_PAY_WECHAT_APPID），此处仅作装配。
 *
 * <p>mock 模式（wechat.mock-enabled=true）：全链路演练（下单/回调/分账/通知），
 * 与真实 API 的协议一致，仅资金不落地；生产必须置 false 并完成真实接入。
 *
 * @author moyun
 */
@Component
@ConfigurationProperties(prefix = "moyun.pay")
public class PayProperties {

    /** 支付通道总开关（false 时下单接口直接拒绝） */
    private boolean enabled = true;

    /** 订单有效期（分钟）：超时未支付自动关单 */
    private int orderExpireMinutes = 30;

    /** 平台服务费率兜底值（0.10=10%）；运行时以 sys_config(pay.platform.fee-rate) 优先 */
    private double platformFeeRate = 0.10;

    /** 安全配置 */
    private Security security = new Security();

    /** 微信支付商户参数 */
    private Wechat wechat = new Wechat();

    public static class Security {
        /** 银行卡号 AES-GCM 加密口令（生产走环境变量注入） */
        private String bankCardEncryptKey;
        /** 单用户银行卡绑定上限 */
        private int bankCardMaxCount = 5;
        /** 绑定银行卡是否强制短信验证码（V11.1 企业级默认开启） */
        private boolean bankCardSmsVerify = true;

        public boolean isBankCardSmsVerify() { return bankCardSmsVerify; }
        public void setBankCardSmsVerify(boolean bankCardSmsVerify) { this.bankCardSmsVerify = bankCardSmsVerify; }
        public String getBankCardEncryptKey() { return bankCardEncryptKey; }
        public void setBankCardEncryptKey(String bankCardEncryptKey) { this.bankCardEncryptKey = bankCardEncryptKey; }
        public int getBankCardMaxCount() { return bankCardMaxCount; }
        public void setBankCardMaxCount(int bankCardMaxCount) { this.bankCardMaxCount = bankCardMaxCount; }
    }

    public static class Wechat {
        /** mock 模拟支付开关（开发/演示 true；生产必须 false 走真实 API） */
        private boolean mockEnabled = true;
        /** 公众号/小程序 AppID */
        private String appId;
        /** 商户号 */
        private String mchId;
        /** 商户 API 证书序列号 */
        private String merchantSerial;
        /** 商户私钥文件路径（apiclient_key.pem） */
        private String privateKeyPath;
        /** APIv3 密钥 */
        private String apiV3Key;
        /** 支付结果回调地址（公网，如 https://your-domain/portal/pay/callback/wechat） */
        private String notifyUrl;

        public boolean isMockEnabled() { return mockEnabled; }
        public void setMockEnabled(boolean mockEnabled) { this.mockEnabled = mockEnabled; }
        public String getAppId() { return appId; }
        public void setAppId(String appId) { this.appId = appId; }
        public String getMchId() { return mchId; }
        public void setMchId(String mchId) { this.mchId = mchId; }
        public String getMerchantSerial() { return merchantSerial; }
        public void setMerchantSerial(String merchantSerial) { this.merchantSerial = merchantSerial; }
        public String getPrivateKeyPath() { return privateKeyPath; }
        public void setPrivateKeyPath(String privateKeyPath) { this.privateKeyPath = privateKeyPath; }
        public String getApiV3Key() { return apiV3Key; }
        public void setApiV3Key(String apiV3Key) { this.apiV3Key = apiV3Key; }
        public String getNotifyUrl() { return notifyUrl; }
        public void setNotifyUrl(String notifyUrl) { this.notifyUrl = notifyUrl; }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getOrderExpireMinutes() { return orderExpireMinutes; }
    public void setOrderExpireMinutes(int orderExpireMinutes) { this.orderExpireMinutes = orderExpireMinutes; }
    public double getPlatformFeeRate() { return platformFeeRate; }
    public void setPlatformFeeRate(double platformFeeRate) { this.platformFeeRate = platformFeeRate; }
    public Security getSecurity() { return security; }
    public void setSecurity(Security security) { this.security = security; }
    public Wechat getWechat() { return wechat; }
    public void setWechat(Wechat wechat) { this.wechat = wechat; }
}
