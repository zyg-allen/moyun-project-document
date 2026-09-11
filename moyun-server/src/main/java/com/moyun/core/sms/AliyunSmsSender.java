package com.moyun.core.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 阿里云短信发送（V11.1，moyun.sms.mock-enabled=false 时生效）
 *
 * <p>真实 API 接入位：配置（AccessKey/签名/模板）已就位，
 * SDK 调用点见下方 TODO。未完成接入前若被激活将拒绝发送（返回 false），
 * 不会伪装成功。
 *
 * <p>接入步骤（生产前完成）：
 * <ol>
 *   <li>pom 引入 aliyun-sdk-dysmsapi（或 alibabacloud-dysmsapi20170525 新版 SDK）</li>
 *   <li>环境变量注入 AccessKey：MOYUN_SMS_ALIYUN_ACCESSKEYID / ACCESSKEYSECRET</li>
 *   <li>补全 TODO 处 SendSmsRequest 组装与客户端调用</li>
 * </ol>
 *
 * @author moyun
 */
@Component
@ConditionalOnProperty(prefix = "moyun.sms", name = "mock-enabled", havingValue = "false")
public class AliyunSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(AliyunSmsSender.class);

    @Autowired
    private SmsProperties smsProperties;

    @Override
    public boolean sendCode(String phone, String code) {
        SmsProperties.Aliyun aliyun = smsProperties.getAliyun();
        if (isBlank(aliyun.getAccessKeyId()) || isBlank(aliyun.getAccessKeySecret())
                || isBlank(aliyun.getSignName()) || isBlank(aliyun.getTemplateCode())) {
            log.error("[sms-aliyun] 配置不完整，拒绝发送（不伪装成功）");
            return false;
        }
        // TODO 真实 API 接入（生产前完成，当前明确失败而非模拟成功）：
        //  1. 构建 DefaultAcsClient(new DefaultProfile("cn-hangzhou", accessKeyId, accessKeySecret))
        //  2. SendSmsRequest req = new SendSmsRequest();
        //     req.setPhoneNumbers(phone);
        //     req.setSignName(aliyun.getSignName());
        //     req.setTemplateCode(aliyun.getTemplateCode());
        //     req.setTemplateParam("{\"code\":\"" + code + "\"}");
        //  3. SendSmsResponse resp = client.getAcsResponse(req);
        //     return "OK".equalsIgnoreCase(resp.getCode());
        log.warn("[sms-aliyun] 真实 SDK 尚未接入（TODO），发送拒绝 phone={}", mask(phone));
        return false;
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }

    private String mask(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
