package com.moyun.core.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 模拟短信发送（V11.1，moyun.sms.mock-enabled=true 时生效）
 *
 * <p>验证码写入服务日志，方便联调；频控/存储/校验/防枚举等全流程
 * 与真实通道完全一致。生产环境必须置 mock-enabled=false。
 *
 * @author moyun
 */
@Component
@ConditionalOnProperty(prefix = "moyun.sms", name = "mock-enabled", havingValue = "true", matchIfMissing = true)
public class MockSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(MockSmsSender.class);

    @Override
    public boolean sendCode(String phone, String code) {
        // 模拟发送：写日志（真实环境严禁记录验证码明文，此处仅为联调可见性）
        log.info("[sms-mock] 发送验证码 -> phone={} code={}（模拟模式，未发送真实短信）", mask(phone), code);
        return true;
    }

    private String mask(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
