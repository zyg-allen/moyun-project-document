package com.moyun.pay.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 公共支付通道配置
 *
 * <p>配置来源：application.yaml 的 moyun.pay 段。生产环境商户密钥等敏感参数
 * 建议通过环境变量注入（如 MOYUN_PAY_WECHAT_APPID），此处仅作装配。
 *
 * <p>mock 模式（wechat.mock-enabled=true）：全链路演练（下单/回调/分账/通知），
 * 与真实 API 的协议一致，仅资金不落地；生产环境自动强制关闭（见 {@link #init()}）。
 *
 * @author moyun
 */
@Component
@ConfigurationProperties(prefix = "moyun.pay")
public class PayProperties {

    private final Environment env;

    public PayProperties(Environment env) {
        this.env = env;
    }

    /** 支付通道总开关（false 时下单接口直接拒绝） */
    private boolean enabled = true;

    /** 订单有效期（分钟）：超时未支付自动关单 */
    private int orderExpireMinutes = 30;

    /** 平台服务费率兜底值（0.10=10%）；运行时以 sys_config(pay.{platformCode}.fee-rate) 优先 */
    private double platformFeeRate = 0.10;

    /** 安全配置 */
    private Security security = new Security();

    /** 微信支付商户参数 */
    private Wechat wechat = new Wechat();

    /** 代付通道配置（提现出金，与收款方向的 wechat 对称） */
    private Payout payout = new Payout();

    /**
     * 生产环境安全守卫：spring.profiles.active 含 prod/production 时，
     * 强制关闭 mock 支付**与 mock 代付**，防止配置遗漏导致"模拟资金"在生产可用。
     *
     * <p>代付关掉 mock 的连带效果（期望行为）：{@code MockPayoutChannel} 因
     * {@code @ConditionalOnProperty} 不匹配而不装配；若此时又未接入真实代付通道，
     * 则 {@code WithdrawOrderServiceImpl} 的渠道集合为空 → 审核通过时**明确拒绝出金**，
     * 即生产"要么真实出金、要么拒绝"，不存在"假装打款成功"的中间态。</p>
     */
    @PostConstruct
    void init() {
        if (env != null) {
            for (String p : env.getActiveProfiles()) {
                if ("prod".equalsIgnoreCase(p) || "production".equalsIgnoreCase(p)) {
                    wechat.mockEnabled = false;
                    payout.mockEnabled = false;
                    break;
                }
            }
        }
    }

    public static class Security {
        /** 银行卡号 AES-GCM 加密口令（生产走环境变量注入） */
        private String bankCardEncryptKey;
        /** 单用户银行卡绑定上限 */
        private int bankCardMaxCount = 5;
        /** 绑定银行卡是否强制短信验证码（企业级默认开启） */
        private boolean bankCardSmsVerify = true;

        public boolean isBankCardSmsVerify() { return bankCardSmsVerify; }
        public void setBankCardSmsVerify(boolean bankCardSmsVerify) { this.bankCardSmsVerify = bankCardSmsVerify; }
        public String getBankCardEncryptKey() { return bankCardEncryptKey; }
        public void setBankCardEncryptKey(String bankCardEncryptKey) { this.bankCardEncryptKey = bankCardEncryptKey; }
        public int getBankCardMaxCount() { return bankCardMaxCount; }
        public void setBankCardMaxCount(int bankCardMaxCount) { this.bankCardMaxCount = bankCardMaxCount; }
    }

    public static class Wechat {
        /** mock 模拟支付开关（开发/演示 true；生产环境由 PayProperties.init() 强制 false） */
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

    /**
     * 代付通道配置（提现出金方向，与 {@link Wechat} 收款方向对称）
     *
     * <p>由 {@code moyun.pay.payout.*} 绑定；对应 Bean 为 {@code PayoutChannel} 实现。</p>
     */
    public static class Payout {
        /**
         * 是否装配模拟代付渠道（{@code MockPayoutChannel}）。
         *
         * <p><b>默认 true</b>：与 {@code wechat.mock-enabled} 取向一致，便于联调环境零配置跑通
         * 提现闭环。生产由 {@link PayProperties#init()} 强制置 false →
         * mock 渠道不装配，未接入真实通道时审核通过会**明确拒绝出金**（不假打款）。</p>
         */
        private boolean mockEnabled = true;

        /**
         * 指定使用的代付渠道标识（可空）。
         *
         * <p>联调 mock 与真实通道并存时用于择一；留空则取装配到的第一个实现。</p>
         */
        private String channel;

        public boolean isMockEnabled() { return mockEnabled; }
        public void setMockEnabled(boolean mockEnabled) { this.mockEnabled = mockEnabled; }
        public String getChannel() { return channel; }
        public void setChannel(String channel) { this.channel = channel; }
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
    public Payout getPayout() { return payout; }
    public void setPayout(Payout payout) { this.payout = payout; }
}
