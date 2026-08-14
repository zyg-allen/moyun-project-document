package com.moyun.portal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.config.redis.RedisCache;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.mapper.PortalUserMapper;
import com.moyun.portal.service.PortalEmailService;
import com.moyun.util.security.SecurityUtils;
import com.moyun.util.string.StringUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 门户邮件服务实现
 *
 * @author moyun
 */
@Slf4j
@Service
public class PortalEmailServiceImpl implements PortalEmailService {

    /** Redis 验证码 key 前缀，按场景区分：moyun:portal:email:code:{type}:{email} */
    private static final String CODE_KEY_PREFIX = "moyun:portal:email:code:";

    /** Redis 限流 key：moyun:portal:email:lock:{email}（同邮箱 60s 内禁止重复发送） */
    private static final String LOCK_KEY_PREFIX = "moyun:portal:email:lock:";

    /** 验证码有效期：5 分钟 */
    private static final Integer CODE_EXPIRE_MINUTES = 5;

    /** 同邮箱发送间隔：60 秒 */
    private static final Integer LOCK_SECONDS = 60;

    /** 邮箱格式正则 */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String from;

    @Autowired
    private RedisCache redisCache;

    @Resource
    private PortalUserMapper portalUserMapper;

    @Override
    public AjaxResult sendCode(String email, String type) {
        // 1. 基础校验
        if (StringUtils.isEmpty(email) || !EMAIL_PATTERN.matcher(email).matches()) {
            return AjaxResult.error("邮箱格式不正确");
        }
        if (!"register".equals(type) && !"reset_password".equals(type)) {
            return AjaxResult.error("不支持的验证码类型");
        }

        // 2. 场景性校验：注册时邮箱不能已存在；找回密码时邮箱必须已存在
        boolean exists = isEmailExists(email);
        if ("register".equals(type) && exists) {
            return AjaxResult.error("该邮箱已注册，请直接登录或找回密码");
        }
        if ("reset_password".equals(type) && !exists) {
            return AjaxResult.error("该邮箱未注册，请检查或先注册");
        }

        // 3. 同邮箱 60s 限流
        String lockKey = LOCK_KEY_PREFIX + email;
        if (redisCache.hasKey(lockKey)) {
            long remain = redisCache.getExpire(lockKey);
            return AjaxResult.error("发送过于频繁，请 " + (remain > 0 ? remain : 60) + " 秒后再试");
        }

        // 4. 邮件服务是否就绪
        if (mailSender == null) {
            log.warn("📧 邮件服务未配置（MAIL_PASSWORD 未设置），无法发送验证码到 {}", email);
            return AjaxResult.error("邮件服务暂未开启，请联系管理员");
        }

        // 5. 生成 6 位数字验证码
        String code = String.format("%06d", (int) (Math.random() * 1000000));

        // 6. 发送邮件（失败不写 Redis，用户可立即重试）
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(email);
            message.setSubject("register".equals(type) ? "墨韵智库 - 注册验证码" : "墨韵智库 - 找回密码验证码");
            message.setText(buildContent(code, type));
            mailSender.send(message);
            log.info("📧 邮件验证码已发送: email={}, type={}", email, type);
        } catch (Exception e) {
            log.error("邮件发送失败: email={}", email, e);
            return AjaxResult.error("邮件发送失败，请稍后重试或检查邮箱地址");
        }

        // 7. 写入 Redis：验证码 5 分钟过期 + 限流锁 60 秒
        redisCache.setCacheObject(CODE_KEY_PREFIX + type + ":" + email, code,
                CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        redisCache.setCacheObject(lockKey, "1", LOCK_SECONDS, TimeUnit.SECONDS);

        return AjaxResult.success("验证码已发送至邮箱，5 分钟内有效");
    }

    @Override
    public boolean verifyCode(String email, String code, String type) {
        if (StringUtils.isEmpty(email) || StringUtils.isEmpty(code) || StringUtils.isEmpty(type)) {
            return false;
        }
        String key = CODE_KEY_PREFIX + type + ":" + email;
        String cached = redisCache.getCacheObject(key);
        if (cached == null) {
            return false;
        }
        return cached.equals(code);
    }

    @Override
    public void consumeCode(String email, String type) {
        redisCache.deleteObject(CODE_KEY_PREFIX + type + ":" + email);
    }

    @Override
    public AjaxResult resetPassword(String email, String code, String newPassword) {
        // 1. 基础校验
        if (StringUtils.isEmpty(email) || !EMAIL_PATTERN.matcher(email).matches()) {
            return AjaxResult.error("邮箱格式不正确");
        }
        if (StringUtils.isEmpty(newPassword) || newPassword.length() < 6 || newPassword.length() > 20) {
            return AjaxResult.error("密码长度必须为 6-20 位");
        }
        if (!newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$")) {
            return AjaxResult.error("密码必须包含大小写字母和数字");
        }

        // 2. 校验验证码（一次性消费，无论成功失败都删除，防暴力枚举）
        boolean ok = verifyCode(email, code, "reset_password");
        consumeCode(email, "reset_password");
        if (!ok) {
            return AjaxResult.error("验证码错误或已过期，请重新获取");
        }

        // 3. 查用户并重置密码
        PortalUser user = portalUserMapper.selectOne(
                new LambdaQueryWrapper<PortalUser>().eq(PortalUser::getEmail, email).last("LIMIT 1"));
        if (user == null) {
            return AjaxResult.error("该邮箱未注册");
        }

        user.setPassword(SecurityUtils.encryptPassword(newPassword));
        portalUserMapper.updateById(user);
        log.info("🔐 用户通过邮箱找回密码重置成功: email={}, username={}", email, user.getUsername());

        return AjaxResult.success("密码重置成功，请使用新密码登录");
    }

    /** 检查邮箱是否已被注册 */
    private boolean isEmailExists(String email) {
        Long count = portalUserMapper.selectCount(
                new LambdaQueryWrapper<PortalUser>().eq(PortalUser::getEmail, email));
        return count != null && count > 0;
    }

    /** 构建邮件正文 */
    private String buildContent(String code, String type) {
        String action = "register".equals(type) ? "注册" : "找回密码";
        return "您好！\n\n"
                + "您正在进行" + action + "操作，验证码为：\n\n"
                + "    " + code + "\n\n"
                + "验证码 5 分钟内有效，请勿向他人泄露。\n"
                + "如非本人操作，请忽略此邮件。\n\n"
                + "—— 墨韵智库";
    }
}
