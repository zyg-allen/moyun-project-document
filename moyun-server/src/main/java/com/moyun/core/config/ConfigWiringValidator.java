package com.moyun.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * 配置接线校验器（启动时断言"被声明但零消费"的配置键）
 *
 * <p><b>存在意义</b>：本项目已多次出现「配置项声明齐全、校验器通过、但运行时读的是另一个键」
 * 或「配置类存在、实际计算不读它」的问题。这类缺陷的共性是：</p>
 * <ul>
 *   <li>配置有默认值 → 启动不报错，校验器看到的是"已配置"；</li>
 *   <li>实际行为由代码里的硬编码/另一属性决定 → 运维改配置不生效；</li>
 *   <li>没有任何症状 → 直到线上事故才被发现。</li>
 * </ul>
 *
 * <p>{@link TokenConfigValidator} 只校验"值是否合法"，无法发现"值是否被消费"。
 * 本类补齐后半段：对每一组已知的"疑似未接线"配置做定向断言，在启动阶段把问题打成
 * {@code ERROR}（生产环境直接阻断启动），避免带病上线。</p>
 *
 * <p><b>维护约定</b>：每新增一条配置 ↔ 消费点绑定关系，就在 {@link #wiringChecks()} 中登记一条；
 * 修复某条接线后，把对应断言改为"校验实际生效值"。禁止删除断言而不修接线。</p>
 *
 * @author moyun
 * @since 2026-09-26
 */
@Slf4j
@Component
public class ConfigWiringValidator implements ApplicationRunner {

    /** 与 ImageFilter 中硬编码的综合评分权重/阈值对比用的容差 */
    private static final double EPS = 1e-9;

    private final Environment env;

    public ConfigWiringValidator(Environment env) {
        this.env = env;
    }

    // ==================== W-1：加密/签名密钥分离 ====================

    @Value("${token.secret:}")
    private String tokenSecret;

    @Value("${token.admin.secret:${token.secret:}}")
    private String adminSecret;

    @Value("${token.portal.secret:${token.secret:}}")
    private String portalSecret;

    // ==================== W-2：图片过滤权重 ====================

    @Value("${image.filter.position-weight:0.4}")
    private double imagePositionWeight;

    @Value("${image.filter.size-weight:0.3}")
    private double imageSizeWeight;

    @Value("${image.filter.complexity-weight:0.3}")
    private double imageComplexityWeight;

    @Value("${image.filter.comprehensive-score-threshold:0.25}")
    private double imageScoreThreshold;

    // ==================== W-3：代码生成器配置 ====================

    @Value("${gen.author:}")
    private String genAuthor;

    // ==================== W-4：敏感口令/密钥（prod 必填） ====================
    // 消费点（已逐个核对）：
    //   cert-no-encrypt-key   → CertSecurityProperties → PortalCreatorCertificationServiceImpl:77（AesGcmUtils.encrypt）
    //   bank-card-encrypt-key → PayProperties.Security  → BankCardServiceImpl:125,127,190（AesGcmUtils.encrypt/decrypt）
    //   moyun.ai.api-key      → VoiceAsrService:60 / AsrStreamRelayHandler:184（后备 LLM/ASR 密钥）
    //   spring.mail.password  → spring.mail → MailChannelStatus（就绪判定，三项配置之一）
    //                           → PortalEmailServiceImpl / SendEmailTool / EmailNodeExecutor（发信）
    //   minio.access/secret   → common/config/MinioConfig → MinioUtils（MinIO 对象存储凭据）

    @Value("${moyun.security.cert-no-encrypt-key:}")
    private String certNoEncryptKey;

    @Value("${moyun.pay.security.bank-card-encrypt-key:}")
    private String bankCardEncryptKey;

    @Value("${moyun.ai.api-key:}")
    private String moyunAiApiKey;

    /**
     * 门户站点域名（消费点：{@code PortalSitemapController} 生成动态 sitemap 的绝对 URL）。
     *
     * <p>默认占位 {@code https://xulin.example.com}；v13.9 起纳入了 prod 阻断断言（见 W-5）。
     * 前端同源要求：moyun-portal 的 {@code VITE_SITE_URL}（构建期注入 canonical/og:url/robots/sitemap）。</p>
     */
    @Value("${moyun.portal.domain:}")
    private String portalDomain;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${minio.access-key:}")
    private String minioAccessKey;

    @Value("${minio.secret-key:}")
    private String minioSecretKey;

    // ==================== D-1：短信 mock 开关（prod 必须显式关闭） ====================

    /**
     * {@code moyun.sms.mock-enabled} 原始值（未配置为 null/空）。
     *
     * <p>刻意**不给默认值**：{@code MockSmsSender} 的 {@code @ConditionalOnProperty} 带
     * {@code matchIfMissing = true}，未配置等同于开启 mock，故这里必须能区分"未配置"与"配了 false"。</p>
     */
    @Value("${moyun.sms.mock-enabled:}")
    private String smsMockEnabled;

    /**
     * "仅生产阻断"类断言的 key 前缀。
     *
     * <p>这类断言在开发环境命中属**预期**（本地默认配置即如此），故记为 info 提示而不报警；
     * 在生产环境命中则阻断启动。用于与常规分级（非 prod 告警 / prod 阻断）区分开，
     * 避免把"本地本就该这样"的项在 dev 日志里刷成 WARN。</p>
     */
    static final String PROD_ONLY_PREFIX = "[仅生产] ";

    @Override
    public void run(ApplicationArguments args) {
        Map<String, BooleanSupplier> checks = wiringChecks();
        boolean prod = isProd();
        int severe = 0;
        int advisory = 0;
        for (Map.Entry<String, BooleanSupplier> entry : checks.entrySet()) {
            boolean ok;
            try {
                ok = entry.getValue().getAsBoolean();
            } catch (Exception e) {
                ok = false;
                log.error("[config-wiring] 校验 [{}] 执行异常：{}", entry.getKey(), e.getMessage());
            }
            if (ok) {
                continue;
            }
            // "仅生产阻断"类：非 prod 命中属本地默认形态，记 info；prod 命中阻断启动
            boolean prodOnly = entry.getKey().startsWith(PROD_ONLY_PREFIX);
            if (prodOnly && !prod) {
                advisory++;
                log.info("[config-wiring] 提示（该要求仅对生产生效）：{}", entry.getKey());
                continue;
            }
            // 常规分级：开发默认口令在非生产可接受（仅告警），生产即配置缺陷（阻断）
            if (prod) {
                severe++;
                log.error("[config-wiring] 校验失败（生产阻断）：{}", entry.getKey());
            } else {
                advisory++;
                log.warn("[config-wiring] 提示（非生产环境可接受，生产将阻断）：{}", entry.getKey());
            }
        }
        String summary = "通过 " + (checks.size() - severe - advisory) + "/" + checks.size()
                + "，提示 " + advisory + "，生产阻断项 " + severe;
        if (severe > 0) {
            throw new IllegalStateException("配置接线校验未通过（" + summary + "）："
                    + "存在「已声明但未被代码消费」或「使用开发默认凭据/危险开关」的配置，"
                    + "运维改动不会生效 / 凭据不安全 / 生产行为不正确。详情见上方 [config-wiring] ERROR 日志。");
        }
        if (advisory > 0) {
            log.warn("[config-wiring] 配置接线校验完成（{}）——生产环境请按 README「环境变量清单」注入真实凭据，"
                    + "否则启动将被阻断", summary);
        } else {
            log.info("[config-wiring] 配置接线校验通过（{}）", summary);
        }
    }

    /**
     * 定向接线断言表：key = 人类可读的问题描述，value = 返回 true 表示"接线正常"
     */
    private Map<String, BooleanSupplier> wiringChecks() {
        Map<String, BooleanSupplier> checks = new LinkedHashMap<>();

        // ---- W-1：管理端/门户端密钥是否真的被各自 TokenService 消费 ------------------
        // 接线现状：TokenService 读 ${token.admin.secret:${token.secret:}}，
        //          PortalTokenService 读 ${token.portal.secret:${token.secret:}}。
        // 断言口径：配置值必须"被消费到"——即解析结果与配置值一致。
        //   若 adminSecret 仍等于 tokenSecret，说明专用密钥未生效（回退发生）。
        // 注：密钥"相同"本身属未分离（安全建议），由 TokenConfigValidator 分级处理
        //     （prod 阻断、非 prod 告警），此处只负责"接线是否通"。
        checks.put("token.admin.secret 已配置但未被 TokenService 消费（解析结果仍为 token.secret，专用密钥未生效）",
                () -> !configured(adminSecret) || adminSecret.equals(tokenSecret));
        checks.put("token.portal.secret 已配置但未被 PortalTokenService 消费（解析结果仍为 token.secret，专用密钥未生效）",
                () -> !configured(portalSecret) || portalSecret.equals(tokenSecret));

        // ---- W-2：图片过滤配置类是否真的被计算逻辑消费 -----------------------------
        // 背景：ext/ai/config/ImageFilterConfig 定义了 positionWeight/sizeWeight/
        //      complexityWeight/comprehensiveScoreThreshold，而 ext/ai/filter/ImageFilter
        //      在 :232/:235 硬编码了同值常量 → 改配置不生效。
        //      断言：一旦配置值与硬编码默认值不同，即说明配置被静默忽略。
        checks.put("image.filter.position-weight 与 ImageFilter 硬编码的 0.4 不一致（配置被忽略）",
                () -> Math.abs(imagePositionWeight - 0.4) < EPS);
        checks.put("image.filter.size-weight 与 ImageFilter 硬编码的 0.3 不一致（配置被忽略）",
                () -> Math.abs(imageSizeWeight - 0.3) < EPS);
        checks.put("image.filter.complexity-weight 与 ImageFilter 硬编码的 0.3 不一致（配置被忽略）",
                () -> Math.abs(imageComplexityWeight - 0.3) < EPS);
        checks.put("image.filter.comprehensive-score-threshold 与 ImageFilter 硬编码的 0.25 不一致（配置被忽略）",
                () -> Math.abs(imageScoreThreshold - 0.25) < EPS);

        // ---- W-3：代码生成器配置是否真的被读取 -------------------------------------
        // 历史缺陷（已修，见 GenConfig 类注释）：字段 static + @Value 取顶层键 +
        // @PropertySource 以 .properties 语义平铺解析 generator.yml，三者叠加"恰好能跑"。
        // 现已改为 `@Value("${gen.xxx}")` + YamlPropertySourceFactory。
        // 本断言保留为**回归守卫**：若有人把键路径改回顶层、或去掉 YAML 工厂，
        // gen.author 会重新读不到，此处即报出。
        checks.put("gen.author 已配置但 GenConfig 读不到（键路径或 YAML 解析方式被改坏）",
                () -> !configured(env.getProperty("gen.author")) || configured(genAuthor));

        // ---- W-4：敏感口令必须"真配"，而不是被开发默认值顶替 -----------------------
        // 分两档，语义不同（这是本组断言的要点，勿混淆）：
        //
        // 【必需档 requiredSecret】空值 = 功能不可用 → prod 阻断、非 prod 告警。
        //   cert / bank-card 两个 AES 口令属此类：AesGcmUtils 已改为空口令即抛异常，
        //   若 prod 漏配，加密入口直接失败（证件号/银行卡号不可写）。
        //   同理"仍是开发默认口令"意味着用公开常量加密 → prod 阻断、非 prod 告警。
        //
        // 【可空档 optionalSecret】空值 = 功能优雅降级，是**安全**状态 → 一律放行。
        //   ai 密钥为空 → VoiceAsrService:97 抛明确异常提示配置；
        //   mail 密码为空 → MailChannelStatus 判为未就绪，三个调用点统一返回
        //     "邮件服务暂未开启/未配置"，**不会**去连 SMTP 拿 535。
        //     注意：这里**不能**写成"mailSender 不装配"——MailSenderAutoConfiguration 只以
        //     spring.mail.host 为条件，而 dev 的 host 是固定值，JavaMailSender 始终存在。
        //     回归测试：MailSenderAutoConfigurationConditionTest。
        //   MinIO 凭据为空 → 上传失败并可降级本地存储。
        //   但"非空且带开发标记"（admin123 / moyun-dev- / todo / example）是**真实的
        //   凭据泄漏形态**，必须报出来：prod 阻断、非 prod 告警。
        checks.put("moyun.security.cert-no-encrypt-key 缺失或为开发默认口令（证件号加密将不可用/用公开口令）",
                () -> requiredSecret(certNoEncryptKey));
        checks.put("moyun.pay.security.bank-card-encrypt-key 缺失或为开发默认口令（银行卡号加密将不可用/用公开口令）",
                () -> requiredSecret(bankCardEncryptKey));
        checks.put("moyun.ai.api-key 使用开发/占位口令（请置空以禁用，或注入真实密钥）",
                () -> optionalSecret(moyunAiApiKey));
        checks.put("spring.mail.password 使用开发/占位口令（请置空以禁用，或注入真实授权码）",
                () -> optionalSecret(mailPassword));
        checks.put("minio.access-key 使用开发/示例口令 admin123（请置空或注入真实凭据）",
                () -> optionalSecret(minioAccessKey));
        checks.put("minio.secret-key 使用开发/示例口令 admin123（请置空或注入真实凭据）",
                () -> optionalSecret(minioSecretKey));

        // ---- D-1：短信通道在生产必须真实（**仅生产阻断**） --------------------------
        // 背景（已核代码）：MockSmsSender 的 @ConditionalOnProperty 带 **matchIfMissing = true**
        // —— 生产配置一旦漏写 moyun.sms 段，mock 实现就会顶上：
        //   MockSmsSender.sendCode 把验证码**明文写入服务日志**且恒返回 true。
        // 后果：① 用户永远收不到验证码 → 注册/找回密码/绑卡/提现全链路不可用；
        //      ② 验证码明文进日志，可被用于绑卡/提现等敏感操作，构成凭据泄漏面。
        // 该"默认落到 mock"的 fail-open 取向与本项目其余安全配置（TokenConfigValidator 在 prod
        // 对危险配置直接抛异常）相反，故此处对齐：prod 命中即阻断启动。
        // 非 prod 命中属本地默认形态（mock 便于联调），仅作 info 提示。
        checks.put(PROD_ONLY_PREFIX + "moyun.sms.mock-enabled 未显式置为 false"
                        + "（默认落到 MockSmsSender：验证码只写日志、用户收不到，且日志含验证码明文）",
                this::smsMockDisabled);

        // ---- W-5：门户站点域名必须真实（**仅生产阻断**） ---------------------------
        // 背景（v13.9）：moyun.portal.domain 默认值是占位域名 https://xulin.example.com，
        // 消费点是 PortalSitemapController 生成的动态 sitemap（绝对 URL）。历史上靠"注释提醒
        // 部署时替换 + 手工同步前端 robots/sitemap"，结果占位域名会随产物上线：
        //   前端已改为构建期注入（VITE_SITE_URL，production 构建缺值即失败）；
        //   后端在此处对齐：prod 下仍是占位/空值 → 阻断启动。
        // 非 prod 命中属本地默认形态（sitemap 在本地无 SEO 意义），记 info 提示。
        checks.put(PROD_ONLY_PREFIX + "moyun.portal.domain 仍是占位/空域名"
                        + "（PORTAL_DOMAIN 未注入：动态 sitemap 会把占位域名发给搜索引擎）",
                () -> requiredSecret(portalDomain));

        return checks;
    }

    /**
     * 短信是否已关闭 mock（即 {@code moyun.sms.mock-enabled=false}）。
     *
     * <p>注意 {@code MockSmsSender} 的 {@code @ConditionalOnProperty} 带
     * {@code matchIfMissing = true}：**未配置**也等同于开启 mock。
     * 故只有显式配置为 false 才算"已关闭"。</p>
     */
    private boolean smsMockDisabled() {
        return smsMockEnabled != null && "false".equalsIgnoreCase(smsMockEnabled.trim());
    }

    /**
     * 必需口令判定：非空且非开发默认口令。
     *
     * <p>用于"缺失即功能不可用"的加密口令：不合格时由 {@link #run} 按 profile 分级处置
     * （prod 阻断启动、非 prod 仅告警）。</p>
     *
     * <p>包级可见以便单测直接覆盖判定语义（见 {@code ConfigWiringValidatorTest}）——
     * 完整的 prod 分支需真实 DB 才能跑到 {@link #run}，故判定逻辑单独可测。</p>
     */
    static boolean requiredSecret(String value) {
        return configured(value) && !looksLikeDevPlaceholder(value);
    }

    /**
     * 可空口令判定：空值是合法的"功能禁用"状态（fail-closed），直接放行；
     * 仅当"非空且形如开发/示例凭据"时判定为不合格。
     */
    static boolean optionalSecret(String value) {
        return !configured(value) || !looksLikeDevPlaceholder(value);
    }

    /** 开发默认口令/占位符特征：不得作为生产的真实凭据 */
    static boolean looksLikeDevPlaceholder(String value) {
        if (value == null) {
            return false;
        }
        String v = value.trim();
        return v.startsWith("moyun-dev-")
                || v.equals("admin123")
                || v.equals("123456")
                || v.startsWith("todo")
                || v.contains("placeholder")
                || v.contains("example");
    }

    private static boolean configured(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean isProd() {
        for (String p : env.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(p) || "production".equalsIgnoreCase(p)) {
                return true;
            }
        }
        return false;
    }
}
