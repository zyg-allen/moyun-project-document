package com.moyun.ext.ai.engine.workflow.node;

import com.moyun.ext.ai.engine.workflow.WorkflowContext;
import com.moyun.ext.ai.engine.workflow.WorkflowNode;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 邮件节点执行器
 *
 * <p>支持发送邮件通知</p>
 * <p>支持的功能：</p>
 * <ul>
 *     <li>发送纯文本邮件</li>
 *     <li>发送HTML邮件</li>
 *     <li>支持抄送和密送</li>
 * </ul>
 *
 * @author laomao
 * @since 2025-12-12
 */
@Slf4j
@Component
public class EmailNodeExecutor extends BaseNodeExecutor {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String defaultFrom;

    @Override
    public String getType() {
        return "email";
    }

    @Override
    public NodeResult execute(WorkflowNode node, WorkflowContext context) {
        Map<String, Object> config = node.getConfig();
        if (config == null) {
            return NodeResult.fail("邮件节点配置为空");
        }

        // 检查邮件服务
        if (mailSender == null) {
            log.warn("📧 邮件服务未配置，模拟发送成功");
            String to = (String) config.get("to");
            String subject = (String) config.get("subject");
            context.setVariable((String) config.getOrDefault("outputVariable", "email_result"), 
                    Map.of("success", true, "message", "邮件服务未配置，模拟发送", "to", to, "subject", subject));
            return NodeResult.success(Map.of("success", true, "simulated", true));
        }

        try {
            // 获取配置
            String to = (String) config.get("to");
            String subject = (String) config.get("subject");
            String content = (String) config.get("content");
            String from = (String) config.getOrDefault("from", defaultFrom);
            String cc = (String) config.get("cc");
            String bcc = (String) config.get("bcc");
            boolean isHtml = Boolean.TRUE.equals(config.get("isHtml"));
            String outputVariable = (String) config.getOrDefault("outputVariable", "email_result");

            if (to == null || to.trim().isEmpty()) {
                return NodeResult.fail("收件人地址为空");
            }
            if (subject == null || subject.trim().isEmpty()) {
                return NodeResult.fail("邮件主题为空");
            }

            // 替换变量
            to = replaceVariables(to, context);
            subject = replaceVariables(subject, context);
            content = content != null ? replaceVariables(content, context) : "";
            if (cc != null) cc = replaceVariables(cc, context);
            if (bcc != null) bcc = replaceVariables(bcc, context);

            log.info("📧 发送邮件: to={}, subject={}", to, subject);

            if (isHtml) {
                sendHtmlMail(from, to, cc, bcc, subject, content);
            } else {
                sendSimpleMail(from, to, cc, bcc, subject, content);
            }

            log.info("📧 邮件发送成功");

            Map<String, Object> result = Map.of(
                    "success", true,
                    "to", to,
                    "subject", subject,
                    "timestamp", System.currentTimeMillis()
            );

            context.setVariable(outputVariable, result);
            return NodeResult.success(result);

        } catch (Exception e) {
            log.error("邮件发送失败", e);
            return NodeResult.fail("邮件发送失败: " + e.getMessage());
        }
    }

    /**
     * 发送简单文本邮件
     */
    private void sendSimpleMail(String from, String to, String cc, String bcc, 
                                 String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to.split(","));
        if (cc != null && !cc.isEmpty()) {
            message.setCc(cc.split(","));
        }
        if (bcc != null && !bcc.isEmpty()) {
            message.setBcc(bcc.split(","));
        }
        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);
    }

    /**
     * 发送HTML邮件
     */
    private void sendHtmlMail(String from, String to, String cc, String bcc,
                               String subject, String content) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(from);
        helper.setTo(to.split(","));
        if (cc != null && !cc.isEmpty()) {
            helper.setCc(cc.split(","));
        }
        if (bcc != null && !bcc.isEmpty()) {
            helper.setBcc(bcc.split(","));
        }
        helper.setSubject(subject);
        helper.setText(content, true);
        mailSender.send(message);
    }
}
