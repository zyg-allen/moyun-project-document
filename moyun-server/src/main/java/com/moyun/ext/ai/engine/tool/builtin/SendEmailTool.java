package com.moyun.ext.ai.engine.tool.builtin;

import com.moyun.ext.ai.engine.tool.ToolContext;
import com.moyun.ext.ai.engine.tool.ToolExecutor;
import com.moyun.ext.ai.engine.tool.ToolResult;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * 邮件发送工具
 *
 * <p>复用前台注册验证码邮件（PortalEmailServiceImpl）和工作流邮件节点
 * （EmailNodeExecutor）相同的 SMTP 通道：spring.mail.* 配置 + JavaMailSender。</p>
 *
 * <p>支持：</p>
 * <ul>
 *     <li>纯文本 / HTML 邮件</li>
 *     <li>多收件人（逗号分隔）、抄送、密送</li>
 * </ul>
 *
 * @author laomao
 */
@Slf4j
@Component
public class SendEmailTool implements ToolExecutor {

    /** 邮箱格式正则（与 PortalEmailServiceImpl 保持一致） */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /** 复用门户邮件服务的 SMTP 通道；未配置（MAIL_PASSWORD 未设置）时注入为 null */
    @Autowired(required = false)
    private JavaMailSender mailSender;

    /** 发件人，默认取 spring.mail.username */
    @Value("${spring.mail.username:}")
    private String defaultFrom;

    @Override
    public String getName() {
        return "send_email";
    }

    @Override
    public String getDescription() {
        return "发送邮件，支持纯文本和HTML格式，支持多收件人、抄送、密送";
    }

    @Override
    public String getParametersSchema() {
        return """
            {
                "type": "object",
                "properties": {
                    "to": {
                        "type": "string",
                        "description": "收件人邮箱，多个用英文逗号分隔"
                    },
                    "subject": {
                        "type": "string",
                        "description": "邮件主题"
                    },
                    "content": {
                        "type": "string",
                        "description": "邮件正文，纯文本或HTML"
                    },
                    "isHtml": {
                        "type": "boolean",
                        "description": "正文是否为HTML格式，默认false",
                        "default": false
                    },
                    "cc": {
                        "type": "string",
                        "description": "抄送人邮箱，多个用英文逗号分隔，可选"
                    },
                    "bcc": {
                        "type": "string",
                        "description": "密送人邮箱，多个用英文逗号分隔，可选"
                    }
                },
                "required": ["to", "subject", "content"]
            }
            """;
    }

    @Override
    public ToolResult execute(ToolContext context, Map<String, Object> params) {
        // 1. 邮件服务是否就绪
        if (mailSender == null) {
            return ToolResult.fail("邮件服务未配置（缺少 spring.mail / MAIL_PASSWORD 配置），无法发送");
        }

        // 2. 参数校验
        String to = asString(params, "to");
        String subject = asString(params, "subject");
        String content = asString(params, "content");
        String cc = asString(params, "cc");
        String bcc = asString(params, "bcc");
        boolean isHtml = Boolean.parseBoolean(String.valueOf(params.getOrDefault("isHtml", "false")));

        if (to == null || to.trim().isEmpty()) {
            return ToolResult.fail("收件人（to）不能为空");
        }
        if (subject == null || subject.trim().isEmpty()) {
            return ToolResult.fail("邮件主题（subject）不能为空");
        }
        if (content == null) {
            content = "";
        }
        for (String addr : to.split(",")) {
            if (!EMAIL_PATTERN.matcher(addr.trim()).matches()) {
                return ToolResult.fail("收件人邮箱格式不正确: " + addr.trim());
            }
        }

        // 3. 发送
        try {
            if (isHtml) {
                sendHtmlMail(to.trim(), trimOrNull(cc), trimOrNull(bcc), subject, content);
            } else {
                sendSimpleMail(to.trim(), trimOrNull(cc), trimOrNull(bcc), subject, content);
            }

            String result = String.format("邮件发送成功：收件人=%s，主题=%s，格式=%s",
                    to, subject, isHtml ? "HTML" : "纯文本");
            log.info("📧 [send_email 工具] {}", result);
            return ToolResult.success(result);

        } catch (Exception e) {
            log.error("📧 [send_email 工具] 邮件发送失败: to={}, subject={}", to, subject, e);
            return ToolResult.fail("邮件发送失败: " + e.getMessage());
        }
    }

    private String trimOrNull(String value) {
        return (value == null || value.trim().isEmpty()) ? null : value.trim();
    }

    /** 发送纯文本邮件 */
    private void sendSimpleMail(String to, String cc, String bcc, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(defaultFrom);
        message.setTo(to.split(","));
        if (cc != null) {
            message.setCc(cc.split(","));
        }
        if (bcc != null) {
            message.setBcc(bcc.split(","));
        }
        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);
    }

    /** 发送HTML邮件 */
    private void sendHtmlMail(String to, String cc, String bcc, String subject, String content) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(defaultFrom);
        helper.setTo(to.split(","));
        if (cc != null) {
            helper.setCc(cc.split(","));
        }
        if (bcc != null) {
            helper.setBcc(bcc.split(","));
        }
        helper.setSubject(subject);
        helper.setText(content, true);
        mailSender.send(message);
    }
}
