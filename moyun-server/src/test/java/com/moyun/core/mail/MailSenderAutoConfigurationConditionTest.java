package com.moyun.core.mail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Boot 邮件自动装配<b>条件</b>的回归测试
 *
 * <p>本类锁定一个容易踩坑的事实，也是历史缺陷（"邮件服务未配置"分支永不可达）的根因：</p>
 *
 * <pre>
 *   MailSenderAutoConfiguration
 *     @ConditionalOnClass({MimeMessage, MimeType, MailSender})
 *     @ConditionalOnMissingBean(MailSender)
 *     @Conditional(MailSenderCondition)          // = HostProperty | JndiNameProperty
 *         HostProperty: @ConditionalOnProperty(prefix = "spring.mail", name = "host")
 * </pre>
 *
 * <p><b>装配只看 {@code spring.mail.host}，与 password/username 无关。</b>
 * 而 {@code application-dev.yaml} 把 {@code spring.mail.host} 固化为 {@code smtp.163.com}，
 * 所以 dev 下 {@code JavaMailSender} 一定存在——任何以
 * {@code mailSender == null} 判定"邮件服务未配置"的代码都是死分支，
 * 请求会真的去连 163 SMTP 并拿到 535 认证失败。</p>
 *
 * <p>结论：就绪判定必须由 {@link MailChannelStatus} 承担（配置层判定）。</p>
 *
 * @author moyun
 */
class MailSenderAutoConfigurationConditionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    MailSenderAutoConfiguration.class,
                    PropertyPlaceholderAutoConfiguration.class))
            .withUserConfiguration(TestBeans.class);

    @Configuration(proxyBeanMethods = false)
    @Import(MailChannelStatus.class)
    static class TestBeans {
    }

    @Test
    @DisplayName("只配 spring.mail.host（password 为空）→ JavaMailSender 仍被装配，但 MailChannelStatus 判为未就绪")
    void hostOnly_senderCreatedButChannelNotReady() {
        runner.withPropertyValues(
                        "spring.mail.host=smtp.163.com",
                        "spring.mail.username=dev@example.com",
                        "spring.mail.password=")
                .run(context -> {
                    assertThat(context).hasSingleBean(JavaMailSender.class);

                    MailChannelStatus status = context.getBean(MailChannelStatus.class);
                    assertThat(status.isReady())
                            .as("password 为空时绝不能判为就绪（否则会去连 SMTP 拿 535 认证失败）")
                            .isFalse();
                    assertThat(status.unavailableReason()).contains("spring.mail.password");
                });
    }

    @Test
    @DisplayName("未配 spring.mail.host → JavaMailSender 不装配，MailChannelStatus 原因指向 host")
    void hostAbsent_senderNotCreated() {
        runner.withPropertyValues(
                        "spring.mail.username=dev@example.com",
                        "spring.mail.password=some-auth-code")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(JavaMailSender.class);

                    MailChannelStatus status = context.getBean(MailChannelStatus.class);
                    assertThat(status.isReady()).isFalse();
                    assertThat(status.unavailableReason()).contains("spring.mail.host");
                });
    }

    @Test
    @DisplayName("host + username + password 齐备 → 通道就绪")
    void fullyConfigured_channelReady() {
        runner.withPropertyValues(
                        "spring.mail.host=smtp.163.com",
                        "spring.mail.username=dev@example.com",
                        "spring.mail.password=some-auth-code")
                .run(context -> {
                    MailChannelStatus status = context.getBean(MailChannelStatus.class);
                    assertThat(status.isReady()).isTrue();
                    assertThat(status.unavailableReason()).isNull();
                    assertThat(status.from()).isEqualTo("dev@example.com");
                });
    }
}
