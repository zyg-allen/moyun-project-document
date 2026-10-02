package com.moyun.pay.config;

import java.math.BigDecimal;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.moyun.core.config.EnvironmentProfile;

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

    /**
     * 付费阅读（target_type=article_paid）是否已开通。
     *
     * <p><b>默认 false</b>：与 {@code PortalTipServiceImpl} 当前实现一致——该分支目前**直接抛
     * "付费阅读功能正在接入支付通道，暂不可用"**（占位逻辑保留在下方注释里）。</p>
     *
     * <p>之所以做成配置而非前端写死：前端据本开关把「解锁全文」按钮置灰并提示"即将开放"，
     * 待支付通道接入后只需改配置（并放开服务端拦截），**无需改前端**。</p>
     */
    private boolean articlePaidEnabled = false;

    public boolean isArticlePaidEnabled() { return articlePaidEnabled; }

    public void setArticlePaidEnabled(boolean articlePaidEnabled) { this.articlePaidEnabled = articlePaidEnabled; }

    /** 平台服务费率兜底值（0.10=10%）；运行时以 sys_config(pay.{platformCode}.fee-rate) 优先 */
    private double platformFeeRate = 0.10;

    /** 安全配置 */
    private Security security = new Security();

    /** 微信支付商户参数 */
    private Wechat wechat = new Wechat();

    /** 代付通道配置（提现出金，与收款方向的 wechat 对称） */
    private Payout payout = new Payout();

    /**
     * 生产环境安全守卫：按 {@link EnvironmentProfile#isProduction(Environment)}（非生产白名单 + 默认按生产）
     * 判定为生产时，强制关闭 mock 支付**与 mock 代付**，防止配置遗漏导致"模拟资金"在生产可用。
     *
     * <p><b>注意其局限</b>：这里改的是**已绑定对象的字段**，而 {@code MockPayoutChannel} 是否装配由
     * {@code @ConditionalOnProperty} 在 bean 定义期读 Environment 决定 —— 本方法改不动它。
     * 因此"代付禁止 mock"不能只靠这里，必须在出金前做**运行时断言**
     * （见 {@code WithdrawOrderServiceImpl#resolvePayoutChannel()}）。收款侧
     * （{@code WechatPayChannel}）读的是本对象字段，故此处对其有效。</p>
     */
    @PostConstruct
    void init() {
        if (EnvironmentProfile.isProduction(env)) {
            wechat.mockEnabled = false;
            payout.mockEnabled = false;
        }
    }

    /**
     * 是否按生产环境对待（供出金等安全守卫做**运行时**判定，不依赖上面的字段改写）。
     *
     * @return true 表示按生产处理
     */
    public boolean isProductionEnvironment() {
        return EnvironmentProfile.isProduction(env);
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

        /**
         * mock 回调**签名密钥**（HMAC-SHA256）。
         *
         * <p><b>为什么必须有</b>：原 mock 验签是 {@code sha256(body)} —— **无任何密钥**，
         * 任何能构造 body 的人都能伪造支付成功回调（客户端可直达
         * {@code /portal/pay/callback/wechat}）。现改为 {@code hex(HMAC-SHA256(secret, body))}
         * 并与请求头 {@code X-Mock-Signature} 恒时比较。</p>
         *
         * <p>留空 ⇒ **fail-closed 拒绝**（不再静默接受）。开发环境在 application-dev.yaml
         * 提供了明确的 dev 默认值；该值已被启动校验列为开发占位凭据，不得用于生产。</p>
         */
        private String mockSignatureSecret;

        public String getMockSignatureSecret() { return mockSignatureSecret; }
        public void setMockSignatureSecret(String mockSignatureSecret) { this.mockSignatureSecret = mockSignatureSecret; }
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

        /**
         * 单笔提现下限（元），默认 1。
         *
         * <p>清单 P2：原先是 {@code WithdrawOrderServiceImpl} 里的**硬编码常量**（1 / 50000），
         * 运营调整额度必须改代码；且前端完全不知道上限，用户填超了才被后端拒绝。
         * 现改为配置项（{@code moyun.pay.payout.withdraw-min/max}），并经账户总览下发给前端做前置校验与提示。</p>
         */
        private BigDecimal withdrawMin = new BigDecimal("1");

        /** 单笔提现上限（元），默认 50000。语义同 {@link #withdrawMin}。 */
        private BigDecimal withdrawMax = new BigDecimal("50000");

        public boolean isMockEnabled() { return mockEnabled; }
        public void setMockEnabled(boolean mockEnabled) { this.mockEnabled = mockEnabled; }
        public String getChannel() { return channel; }
        public void setChannel(String channel) { this.channel = channel; }
        public BigDecimal getWithdrawMin() { return withdrawMin; }
        public void setWithdrawMin(BigDecimal withdrawMin) { this.withdrawMin = withdrawMin; }
        public BigDecimal getWithdrawMax() { return withdrawMax; }
        public void setWithdrawMax(BigDecimal withdrawMax) { this.withdrawMax = withdrawMax; }
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
