package com.moyun.core.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ConfigWiringValidator} 敏感口令判定语义单测
 *
 * <p>为什么需要这组测试：{@code ConfigWiringValidator} 是 {@code ApplicationRunner}，
 * 只在 ApplicationContext 完整 refresh 之后才执行；而本项目测试环境需要真实的
 * MySQL/Redis 才能完成 refresh（DataSource 创建即校验连接）。因此 prod 分支无法通过
 * 集成测试覆盖到。本类改为直接锁定"判定语义"这一纯函数部分，
 * 保证以下三档行为不会被后续改动破坏：</p>
 *
 * <ul>
 *   <li><b>必需档</b>（加密口令）：空值不合格；开发默认口令不合格；真实值合格。</li>
 *   <li><b>可空档</b>（AI/Mail/MinIO 凭据）：空值<b>合格</b>（功能优雅降级 = fail-closed，是安全状态）；
 *       开发/示例口令不合格；真实值合格。</li>
 * </ul>
 *
 * @author moyun
 */
class ConfigWiringValidatorTest {

    // ==================== 开发默认口令识别 ====================

    @Test
    @DisplayName("开发/占位口令可被识别（这些值绝不能进生产）")
    void detectsDevPlaceholders() {
        assertTrue(ConfigWiringValidator.looksLikeDevPlaceholder("moyun-dev-cert-key-2026"));
        assertTrue(ConfigWiringValidator.looksLikeDevPlaceholder("moyun-dev-pay-bank-card-key-2026"));
        assertTrue(ConfigWiringValidator.looksLikeDevPlaceholder("admin123"));
        assertTrue(ConfigWiringValidator.looksLikeDevPlaceholder("123456"));
        assertTrue(ConfigWiringValidator.looksLikeDevPlaceholder("todo-wx-appid"));
        assertTrue(ConfigWiringValidator.looksLikeDevPlaceholder("todo-aliyun-access-key-id"));
        assertTrue(ConfigWiringValidator.looksLikeDevPlaceholder("placeholder-value"));
        assertTrue(ConfigWiringValidator.looksLikeDevPlaceholder("your-example-key"));
    }

    @Test
    @DisplayName("真实凭据不被误判为开发口令")
    void realSecretsAreNotFlagged() {
        assertFalse(ConfigWiringValidator.looksLikeDevPlaceholder("Zq7#kL9$mX2pR4tV6wY8"));
        assertFalse(ConfigWiringValidator.looksLikeDevPlaceholder("sk-live-abcdef123456"));
        // null 安全：调用方通常已先判空，这里只保证不抛 NPE
        assertFalse(ConfigWiringValidator.looksLikeDevPlaceholder(null));
    }

    // ==================== 必需档：加密口令 ====================

    @Test
    @DisplayName("必需档：空口令必须判定为不合格（否则会用公开常量加密）")
    void requiredSecretRejectsEmpty() {
        assertFalse(ConfigWiringValidator.requiredSecret(null));
        assertFalse(ConfigWiringValidator.requiredSecret(""));
        assertFalse(ConfigWiringValidator.requiredSecret("   "));
    }

    @Test
    @DisplayName("必需档：开发默认口令判定为不合格（prod 将阻断启动）")
    void requiredSecretRejectsDevPlaceholder() {
        assertFalse(ConfigWiringValidator.requiredSecret("moyun-dev-cert-key-2026"));
        assertFalse(ConfigWiringValidator.requiredSecret("moyun-dev-pay-bank-card-key-2026"));
    }

    @Test
    @DisplayName("必需档：真实口令判定为合格")
    void requiredSecretAcceptsReal() {
        assertTrue(ConfigWiringValidator.requiredSecret("Zq7#kL9$mX2pR4tV6wY8"));
    }

    // ==================== 可空档：AI/Mail/MinIO 凭据 ====================

    @Test
    @DisplayName("可空档：空值是合法的功能禁用状态（fail-closed），必须放行")
    void optionalSecretAcceptsEmpty() {
        // 这三类为空时下游都有代码级兜底：
        //   ai 密钥空 → VoiceAsrService 抛明确异常提示配置
        //   mail 密码空 → MailChannelStatus 判为未就绪，三个调用点统一返回"邮件服务暂未开启"
        //     （注意：mailSender **仍会被装配**，自动装配只以 spring.mail.host 为条件；
        //      详见 MailSenderAutoConfigurationConditionTest）
        //   MinIO 凭据空 → 上传失败并可降级本地存储
        // 因此"空"不应被当作配置缺陷报警，否则会掩盖真正的凭据泄漏（admin123 之类的非空值）。
        assertTrue(ConfigWiringValidator.optionalSecret(null));
        assertTrue(ConfigWiringValidator.optionalSecret(""));
        assertTrue(ConfigWiringValidator.optionalSecret("   "));
    }

    @Test
    @DisplayName("可空档：非空的开发/示例凭据判定为不合格（这才是真实泄漏形态）")
    void optionalSecretRejectsDevPlaceholder() {
        assertFalse(ConfigWiringValidator.optionalSecret("admin123"));
        assertFalse(ConfigWiringValidator.optionalSecret("123456"));
        assertFalse(ConfigWiringValidator.optionalSecret("moyun-dev-only-secret-key-do-not-use-in-prod-env-at-least-64-chars-2026"));
        assertFalse(ConfigWiringValidator.optionalSecret("todo-aliyun-access-key-id"));
    }

    @Test
    @DisplayName("可空档：真实凭据判定为合格")
    void optionalSecretAcceptsReal() {
        assertTrue(ConfigWiringValidator.optionalSecret("sk-live-abcdef123456"));
        assertTrue(ConfigWiringValidator.optionalSecret("9f8e7d6c5b4a"));
    }

    // ==================== W-5：门户站点域名（v13.9 新增） ====================

    @Test
    @DisplayName("站点域名：占位域名/空值在 prod 会被阻断（避免 sitemap 把占位域名发给搜索引擎）")
    void portalDomainMustBeReal() {
        // 默认占位（application.yaml 的 PORTAL_DOMAIN 缺省值）
        assertFalse(ConfigWiringValidator.requiredSecret("https://xulin.example.com"));
        // 其他占位形态
        assertFalse(ConfigWiringValidator.requiredSecret("https://your-domain.example"));
        assertFalse(ConfigWiringValidator.requiredSecret(""));
        assertFalse(ConfigWiringValidator.requiredSecret("   "));
        // 真实域名合格（含内网/测试域名——判定只针对"占位特征"，不猜真实域名长什么样）
        assertTrue(ConfigWiringValidator.requiredSecret("https://xulinzhixing.com"));
        assertTrue(ConfigWiringValidator.requiredSecret("https://portal.moyun.internal"));
    }

    @Test
    @DisplayName("站点域名：占位特征与前端构建期自检口径互补")
    void portalDomainPlaceholderMarkers() {
        // 前端拦的是 "xulin.example.com" 字面量（构建产物自检），后端这里用通用占位特征，
        // 覆盖 example / placeholder / todo 等写法
        assertTrue(ConfigWiringValidator.looksLikeDevPlaceholder("https://xulin.example.com"));
        assertTrue(ConfigWiringValidator.looksLikeDevPlaceholder("https://placeholder.site"));
        assertFalse(ConfigWiringValidator.looksLikeDevPlaceholder("https://xulinzhixing.com"));
    }
}
