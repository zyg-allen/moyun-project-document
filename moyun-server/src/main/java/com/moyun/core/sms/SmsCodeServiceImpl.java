package com.moyun.core.sms;

import com.moyun.core.config.redis.RedisCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * 短信验证码服务实现（V11.1）
 *
 * <p>企业级安全要点：
 * <ul>
 *   <li>频控：同一手机号 60s 发送间隔 + 每日上限（Redis 计数）</li>
 *   <li>一次性：验证通过立即删除，不可重放</li>
 *   <li>防枚举：连续错误 N 次直接作废当前验证码</li>
 *   <li>校验恒定语义：不区分"验证码不存在/已过期/错误"，统一返回失败，防探测</li>
 * </ul>
 *
 * @author moyun
 */
@Service
public class SmsCodeServiceImpl implements SmsCodeService {

    private static final Logger log = LoggerFactory.getLogger(SmsCodeServiceImpl.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String KEY_CODE = "sms:code:%s:%s";
    private static final String KEY_INTERVAL = "sms:interval:%s:%s";
    private static final String KEY_DAILY = "sms:daily:%s:%s";
    private static final String KEY_ATTEMPTS = "sms:attempts:%s:%s";

    @Autowired
    private SmsProperties smsProperties;

    @Autowired
    private SmsSender smsSender;

    @Autowired
    private RedisCache redisCache;

    @Override
    public void sendCode(String phone, String scene) {
        if (!smsProperties.isEnabled()) {
            throw new IllegalStateException("短信服务未开启（moyun.sms.enabled=false）");
        }
        validatePhone(phone);
        if (scene == null || scene.isBlank()) {
            throw new IllegalArgumentException("业务场景不能为空");
        }

        // 1. 间隔频控
        String intervalKey = String.format(KEY_INTERVAL, scene, phone);
        if (redisCache.getCacheObject(intervalKey) != null) {
            throw new IllegalStateException("发送过于频繁，请 " + smsProperties.getSendIntervalSeconds() + " 秒后再试");
        }
        // 2. 日限额
        String dailyKey = String.format(KEY_DAILY, scene, phone);
        Integer dailyCount = redisCache.getCacheObject(dailyKey);
        if (dailyCount != null && dailyCount >= smsProperties.getDailyLimit()) {
            throw new IllegalStateException("今日发送次数已达上限");
        }

        // 3. 生成 6 位数字验证码（SecureRandom，杜绝可预测序列）
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));

        // 4. 发送（渠道 SPI：mock 或真实）
        boolean sent = smsSender.sendCode(phone, code);
        if (!sent) {
            throw new IllegalStateException("短信发送失败，请稍后重试");
        }

        // 5. 落 Redis（code TTL = 有效期；interval TTL = 发送间隔；daily TTL = 当日剩余秒数）
        String codeKey = String.format(KEY_CODE, scene, phone);
        redisCache.setCacheObject(codeKey, code,
                smsProperties.getCodeExpireMinutes(), TimeUnit.MINUTES);
        redisCache.setCacheObject(intervalKey, "1",
                smsProperties.getSendIntervalSeconds(), TimeUnit.SECONDS);
        if (dailyCount == null) {
            long secondsToMidnight = java.time.Duration.between(
                    java.time.LocalDateTime.now(), java.time.LocalDate.now().plusDays(1).atStartOfDay()).getSeconds();
            redisCache.setCacheObject(dailyKey, 1, (int) secondsToMidnight, TimeUnit.SECONDS);
        } else {
            redisCache.setCacheObject(dailyKey, dailyCount + 1);
        }
        // 重置错误计数
        redisCache.deleteObject(String.format(KEY_ATTEMPTS, scene, phone));
        log.info("[sms] 验证码已发送 phone={} scene={} expireMinutes={}",
                mask(phone), scene, smsProperties.getCodeExpireMinutes());
    }

    @Override
    public boolean verifyCode(String phone, String scene, String code) {
        if (phone == null || scene == null || code == null || code.isBlank()) {
            return false;
        }
        String codeKey = String.format(KEY_CODE, scene, phone);
        String stored = redisCache.getCacheObject(codeKey);
        if (stored == null) {
            // 不存在/已过期：统一失败
            return false;
        }
        String attemptsKey = String.format(KEY_ATTEMPTS, scene, phone);
        if (stored.equals(code)) {
            // 一次性消费：通过即删除，防重放
            redisCache.deleteObject(codeKey);
            redisCache.deleteObject(attemptsKey);
            log.info("[sms] 验证码校验通过 phone={} scene={}", mask(phone), scene);
            return true;
        }
        // 防枚举：累计错误次数，超阈值作废
        Integer attempts = redisCache.getCacheObject(attemptsKey);
        int next = (attempts == null ? 0 : attempts) + 1;
        if (next >= smsProperties.getMaxVerifyAttempts()) {
            redisCache.deleteObject(codeKey);
            redisCache.deleteObject(attemptsKey);
            log.warn("[sms] 验证码连续错误达上限，已作废 phone={} scene={}", mask(phone), scene);
        } else {
            redisCache.setCacheObject(attemptsKey, next);
        }
        return false;
    }

    private void validatePhone(String phone) {
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new IllegalArgumentException("手机号格式不正确");
        }
    }

    private String mask(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
