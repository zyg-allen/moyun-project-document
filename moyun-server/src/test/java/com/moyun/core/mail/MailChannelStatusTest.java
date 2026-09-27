package com.moyun.core.mail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link MailChannelStatus} 就绪判定语义单测
 *
 * <p>为什么需要这组测试：{@code MailChannelStatus} 的判定规则要与
 * Spring Boot {@code MailSenderAutoConfiguration} 的装配条件<b>解耦</b>——
 * 后者的条件是 {@code spring.mail.host} 存在（另有 jndi-name 分支），
 * <b>不校验 password</b>。因此"JavaMailSender 是否存在"不能作为"邮件是否可用"的判据。</p>
 *
 * <p>本类锁定三条规则，防止将来有人把判定改回 {@code sender == null}：
 * ① 无 sender（host 缺失）→ 未就绪；② username 空 → 未就绪；③ password 空 → 未就绪。</p>
 *
 * @author moyun
 */
class MailChannelStatusTest {

    @SuppressWarnings("unchecked")
    private static MailChannelStatus status(JavaMailSender sender, String host,
                                            String username, String password) {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        return new MailChannelStatus(provider, host, username, password);
    }

    @Test
    @DisplayName("JavaMailSender 未装配（host 缺失）→ 未就绪，原因指向 spring.mail.host")
    void senderMissing_notReady() {
        MailChannelStatus s = status(null, "", "dev@example.com", "auth-code");

        assertFalse(s.isReady());
        assertTrue(s.unavailableReason().contains("spring.mail.host"),
                "原因必须指向 host 未配置，实际: " + s.unavailableReason());
    }

    @Test
    @DisplayName("sender 已装配但 password 为空（dev 默认形态）→ 未就绪，不得误判为可用")
    void passwordBlank_notReady() {
        MailChannelStatus s = status(mock(JavaMailSender.class), "smtp.163.com",
                "dev@example.com", "");

        assertFalse(s.isReady(),
                "host 存在时 JavaMailSender 一定被装配，password 为空必须判为未就绪，否则会去连 SMTP 拿 535");
        assertTrue(s.unavailableReason().contains("spring.mail.password"),
                "原因必须指向 spring.mail.password，实际: " + s.unavailableReason());
    }

    @Test
    @DisplayName("password 为 null 与空白同样判为未就绪")
    void passwordNullAndBlank_notReady() {
        JavaMailSender sender = mock(JavaMailSender.class);

        assertFalse(status(sender, "smtp.163.com", "dev@example.com", null).isReady());
        assertFalse(status(sender, "smtp.163.com", "dev@example.com", "   ").isReady());
    }

    @Test
    @DisplayName("username 为空 → 未就绪，原因指向 spring.mail.username")
    void usernameBlank_notReady() {
        MailChannelStatus s = status(mock(JavaMailSender.class), "smtp.163.com", "", "auth-code");

        assertFalse(s.isReady());
        assertTrue(s.unavailableReason().contains("spring.mail.username"),
                "原因必须指向 spring.mail.username，实际: " + s.unavailableReason());
    }

    @Test
    @DisplayName("三项齐备 → 就绪，unavailableReason 为 null，from() 返回 username")
    void fullyConfigured_ready() {
        MailChannelStatus s = status(mock(JavaMailSender.class), "smtp.163.com",
                "dev@example.com", "auth-code");

        assertTrue(s.isReady());
        assertNull(s.unavailableReason());
        assertEquals("dev@example.com", s.from());
        assertEquals("smtp.163.com", s.host());
    }

    @Test
    @DisplayName("from()/host() 在未配置时返回空串而不是 null（避免下游 NPE）")
    void fromAndHost_neverNull() {
        MailChannelStatus s = status(null, null, null, null);

        assertEquals("", s.from());
        assertEquals("", s.host());
    }
}
