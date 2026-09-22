package com.moyun.core.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 阿里云短信发送（moyun.sms.mock-enabled=false 时生效）
 *
 * <p>结构已就位：配置校验 → 请求组装（签名/模板参数）→ 客户端调用 →
 * 响应解析 → 异常处理 → 发送结果记录。真实 SDK 调用点以
 * {@code todo：配置第三方：} 注释标识，接入前不会伪装发送成功。
 *
 * <p>接入步骤（生产前完成）：
 * <ol>
 *   <li>pom 引入 aliyun-sdk-dysmsapi（或 alibabacloud-dysmsapi20170525 新版 SDK）</li>
 *   <li>环境变量注入 AccessKey：MOYUN_SMS_ALIYUN_ACCESSKEYID / ACCESSKEYSECRET，
 *       并将 sign-name / template-code 替换为真实签名与模板 ID</li>
 *   <li>放开 {@link #sendCode} 内 {@code todo：配置第三方：} 标记处的 SDK 骨架代码</li>
 * </ol>
 *
 * @author moyun
 */
@Component
@ConditionalOnProperty(prefix = "moyun.sms", name = "mock-enabled", havingValue = "false")
public class AliyunSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(AliyunSmsSender.class);

    /** 配置占位值前缀（application-dev.yaml 中 todo-xxx 视同未配置） */
    private static final String PLACEHOLDER_PREFIX = "todo";

    @Autowired
    private SmsProperties smsProperties;

    @Override
    public boolean sendCode(String phone, String code) {
        SmsProperties.Aliyun aliyun = smsProperties.getAliyun();

        // 1. 通道配置完整性校验：缺失或占位值均视为未配置，明确失败（不静默假成功）
        if (isChannelNotConfigured(aliyun)) {
            log.warn("[sms-aliyun] 通道未配置：accessKeyId/accessKeySecret/signName/templateCode "
                    + "存在缺失或 todo 占位值，拒绝发送 phone={}", mask(phone));
            return false;
        }

        // 2. 请求组装：模板参数（验证码），签名/模板号/手机号在下方 SDK 骨架中组装
        String templateParam = buildTemplateParam(code);
        log.debug("[sms-aliyun] 组装发送请求 phone={} signName={} templateCode={}",
                mask(phone), aliyun.getSignName(), aliyun.getTemplateCode());

        try {
            // 3. 客户端调用 + 4. 响应解析
            // todo：配置第三方：阿里云短信 AccessKey/签名/模板ID 配置后启用真实发送
            // （pom 引入 com.aliyun:dysmsapi20170525 或 aliyun-sdk-dysmsapi 后，放开以下骨架）
            //
            // --- 请求组装 ---
            // com.aliyun.dysmsapi20170525.models.SendSmsRequest req =
            //         new com.aliyun.dysmsapi20170525.models.SendSmsRequest()
            //                 .setPhoneNumbers(phone)              // 接收手机号
            //                 .setSignName(aliyun.getSignName())   // 短信签名
            //                 .setTemplateCode(aliyun.getTemplateCode()) // 模板 CODE
            //                 .setTemplateParam(templateParam);    // {"code":"xxxxxx"}
            //
            // --- 客户端初始化（建议作为本类字段懒加载，单例复用） ---
            // com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config()
            //         .setAccessKeyId(aliyun.getAccessKeyId())
            //         .setAccessKeySecret(aliyun.getAccessKeySecret())
            //         .setEndpoint(aliyun.getEndpoint());
            // com.aliyun.dysmsapi20170525.Client client =
            //         new com.aliyun.dysmsapi20170525.Client(config);
            //
            // --- 客户端调用 + 响应解析 ---
            // com.aliyun.dysmsapi20170525.models.SendSmsResponse resp = client.sendSms(req);
            // boolean ok = "OK".equalsIgnoreCase(resp.getBody().getCode());
            //
            // --- 发送结果记录（成功/失败均落日志，含 BizId 便于对账，验证码明文不落日志） ---
            // if (ok) {
            //     log.info("[sms-aliyun] 发送成功 phone={} bizId={}", mask(phone), resp.getBody().getBizId());
            //     return true;
            // }
            // log.warn("[sms-aliyun] 发送失败 phone={} code={} message={}",
            //         mask(phone), resp.getBody().getCode(), resp.getBody().getMessage());
            // return false;

            // SDK 未接入期间的明确失败（防止误以为已真实发送）
            log.warn("[sms-aliyun] 真实 SDK 未接入（见 todo：配置第三方： 标记），发送拒绝 "
                    + "phone={} signName={} templateCode={}",
                    mask(phone), aliyun.getSignName(), aliyun.getTemplateCode());
            return false;
        } catch (Exception e) {
            // 5. 异常处理：网络/SDK 异常不向上抛（SmsCodeServiceImpl 统一转为"发送失败"提示），
            //    记录完整异常便于排查，敏感信息（手机号）脱敏
            log.error("[sms-aliyun] 发送异常 phone={} err={}", mask(phone), e.getMessage(), e);
            return false;
        } finally {
            // 6. 发送结果记录：templateParam 含验证码明文，仅 DEBUG 级别且脱敏输出可用，
            //    此处只记录流程结束，不记录验证码
            log.debug("[sms-aliyun] 发送流程结束 phone={}", mask(phone));
        }
    }

    /**
     * 通道是否未配置：任一必要参数为空，或仍为 todo-xxx 占位值
     */
    private boolean isChannelNotConfigured(SmsProperties.Aliyun aliyun) {
        return isBlankOrPlaceholder(aliyun.getAccessKeyId())
                || isBlankOrPlaceholder(aliyun.getAccessKeySecret())
                || isBlankOrPlaceholder(aliyun.getSignName())
                || isBlankOrPlaceholder(aliyun.getTemplateCode());
    }

    private boolean isBlankOrPlaceholder(String s) {
        return s == null || s.isBlank() || s.trim().toLowerCase().startsWith(PLACEHOLDER_PREFIX);
    }

    /**
     * 组装阿里云短信模板参数（验证码场景：{"code":"123456"}）
     * <p>code 仅 6 位数字（SmsCodeServiceImpl 生成），无 JSON 注入风险
     */
    private String buildTemplateParam(String code) {
        return "{\"code\":\"" + code + "\"}";
    }

    private String mask(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
