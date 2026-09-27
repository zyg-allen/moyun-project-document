package com.moyun.core.mail;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 邮件（SMTP）通道就绪判定 —— 邮件链路的<b>单一事实来源</b>
 *
 * <h3>为什么必须单独判定，而不能只看 {@code JavaMailSender} 是否为 null</h3>
 *
 * <p>Spring Boot 的 {@code MailSenderAutoConfiguration} 上的条件是
 * {@code @ConditionalOnProperty(prefix = "spring.mail", name = "host")}
 * （另有 {@code jndi-name} 分支），<b>不校验 password</b>。
 * 而 {@code application-dev.yaml} 把 {@code spring.mail.host} 写成了固定值
 * {@code smtp.163.com}，于是：</p>
 *
 * <pre>
 *   MAIL_PASSWORD 为空  →  JavaMailSender 依旧被装配（mailSender != null）
 * </pre>
 *
 * <p>历史实现中三个调用点（{@code PortalEmailServiceImpl}、{@code SendEmailTool}、
 * {@code EmailNodeExecutor}）都以 {@code mailSender == null} 作为"邮件服务未配置"的判据，
 * 该分支在实际运行中<b>永远不可达</b>：请求会真的去连 163 SMTP，拿到 535 认证失败，
 * 再被包装为"邮件发送失败，请稍后重试或检查邮箱地址"——把<b>服务端未配置</b>说成
 * <b>用户邮箱地址有问题</b>。既误导终端用户，也让本地联调排查方向跑偏。</p>
 *
 * <h3>判定规则</h3>
 * <ol>
 *   <li>{@code JavaMailSender} 未装配（{@code spring.mail.host} 为空）→ 未就绪；</li>
 *   <li>{@code spring.mail.username} 为空（无发件人）→ 未就绪；</li>
 *   <li>{@code spring.mail.password} 为空（无 SMTP 授权码）→ 未就绪。</li>
 * </ol>
 *
 * <p>规则 3 与本项目 {@code spring.mail.properties.mail.smtp.auth=true} 的契约对齐；
 * 若将来接入免认证内网中继（{@code smtp.auth=false}），需放开该条。</p>
 *
 * <p>本类只做"能不能发"的判定，不做任何发送动作，也不吞异常——
 * 调用方据此在<b>真正连接 SMTP 之前</b>fail-fast，避免无效往返与误导性错误文案。</p>
 *
 * @author moyun
 */
@Component
public class MailChannelStatus {

    /** 延迟解析：autoconfiguration 注册的 JavaMailSender 在构造期不保证已就绪 */
    private final ObjectProvider<JavaMailSender> senderProvider;

    private final String host;
    private final String username;
    private final String password;

    public MailChannelStatus(ObjectProvider<JavaMailSender> senderProvider,
                            @Value("${spring.mail.host:}") String host,
                            @Value("${spring.mail.username:}") String username,
                            @Value("${spring.mail.password:}") String password) {
        this.senderProvider = senderProvider;
        this.host = host;
        this.username = username;
        this.password = password;
    }

    /**
     * 是否具备发信条件。
     *
     * @return {@code true} 表示可以调用 {@link #sender()} 发信
     */
    public boolean isReady() {
        return unavailableReason() == null;
    }

    /**
     * 未就绪原因（用于日志与接口提示）；就绪时返回 {@code null}。
     *
     * @return 人类可读的缺失项描述
     */
    public String unavailableReason() {
        if (sender() == null) {
            return "spring.mail.host 未配置（JavaMailSender 未装配）";
        }
        if (!StringUtils.hasText(username)) {
            return "spring.mail.username 未配置（缺少发件人）";
        }
        if (!StringUtils.hasText(password)) {
            return "spring.mail.password 未配置（缺少 SMTP 授权码；仅配 host 不会使 JavaMailSender 失效）";
        }
        return null;
    }

    /**
     * 已装配的 {@link JavaMailSender}；未装配时返回 {@code null}。
     * <p>调用前请先用 {@link #isReady()} 判定。</p>
     */
    public JavaMailSender sender() {
        return senderProvider.getIfAvailable();
    }

    /**
     * 发件人地址（即 {@code spring.mail.username}）；未配置时返回空串。
     */
    public String from() {
        return username == null ? "" : username;
    }

    /**
     * 当前配置的 SMTP host，仅用于日志排查。
     */
    public String host() {
        return host == null ? "" : host;
    }
}
