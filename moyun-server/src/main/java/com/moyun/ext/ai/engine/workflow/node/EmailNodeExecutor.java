package com.moyun.ext.ai.engine.workflow.node;

import com.moyun.core.mail.MailChannelStatus;
import com.moyun.ext.ai.engine.workflow.WorkflowContext;
import com.moyun.ext.ai.engine.workflow.WorkflowNode;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.SimpleMailMessage;
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

    /** 邮件通道就绪判定（单一事实来源，详见 {@link MailChannelStatus} 类注释） */
    @Autowired
    private MailChannelStatus mailChannelStatus;

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

        // 检查邮件通道是否就绪（配置层判定，先于真正连接 SMTP）
        // 邮件通道未就绪时必须明确失败，不得伪装成"模拟发送成功"（fail-open 假成功）：
        // 否则工作流上游会以为邮件已送达，而实际上什么都没发；
        // 因此这里明确失败，由调用方/编排决定是否走错误分支。
        String notReady = mailChannelStatus.unavailableReason();
        if (notReady != null) {
            log.warn("📧 邮件节点无法执行，邮件通道未就绪：{}", notReady);
            return NodeResult.fail("邮件服务未配置（" + notReady + "），邮件节点无法执行");
        }

        try {
            // 获取配置
            String to = (String) config.get("to");
            String subject = (String) config.get("subject");
            String content = (String) config.get("content");
            String from = (String) config.getOrDefault("from", mailChannelStatus.from());
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

        } catch (MailAuthenticationException e) {
            log.error("📧 邮件服务认证失败: host={}, username={}",
                    mailChannelStatus.host(), mailChannelStatus.from(), e);
            return NodeResult.fail("邮件服务认证失败（MAIL_USERNAME / MAIL_PASSWORD 配置有误），请联系管理员");
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
        mailChannelStatus.sender().send(message);
    }

    /**
     * 发送HTML邮件
     */
    private void sendHtmlMail(String from, String to, String cc, String bcc,
                               String subject, String content) throws Exception {
        MimeMessage message = mailChannelStatus.sender().createMimeMessage();
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
        mailChannelStatus.sender().send(message);
    }
}
