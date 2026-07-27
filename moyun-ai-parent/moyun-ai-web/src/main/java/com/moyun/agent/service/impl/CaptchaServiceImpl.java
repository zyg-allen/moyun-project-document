package com.moyun.agent.service.impl;

import com.moyun.agent.constant.RedisKeys;
import com.moyun.agent.dto.CaptchaResponse;
import com.moyun.agent.service.CaptchaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务实现类
 *
 * <p>生成图形验证码并存储到Redis</p>
 *
 * @author laomao
 */
@Slf4j
@Service
public class CaptchaServiceImpl implements CaptchaService {

    /** 验证码长度 */
    private static final int CAPTCHA_LENGTH = 4;

    /** 验证码图片宽度 */
    private static final int CAPTCHA_WIDTH = 120;

    /** 验证码图片高度 */
    private static final int CAPTCHA_HEIGHT = 42;

    /** 验证码字符集（去掉容易混淆的字符如0、O、1、I、L） */
    private static final String CAPTCHA_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public CaptchaResponse generateCaptcha() {
        // 生成随机验证码
        String captchaCode = generateRandomCode();
        String captchaKey = UUID.randomUUID().toString().replace("-", "");

        // 存储到Redis，5分钟过期
        redisTemplate.opsForValue().set(
            RedisKeys.captcha(captchaKey),
            captchaCode.toUpperCase(),
            RedisKeys.CAPTCHA_EXPIRE_MINUTES,
            TimeUnit.MINUTES
        );

        // 生成验证码图片
        String captchaImage = generateCaptchaImage(captchaCode);

        CaptchaResponse response = new CaptchaResponse();
        response.setCaptchaKey(captchaKey);
        response.setCaptchaImage(captchaImage);

        log.debug("生成验证码: key={}, code={}", captchaKey, captchaCode);
        return response;
    }

    @Override
    public boolean verifyCaptcha(String captchaKey, String captchaCode) {
        if (captchaKey == null || captchaCode == null) {
            return false;
        }

        String storedCode = redisTemplate.opsForValue().get(RedisKeys.captcha(captchaKey));
        if (storedCode == null) {
            log.warn("验证码已过期或不存在: key={}", captchaKey);
            return false;
        }

        // 验证后删除，防止重复使用
        redisTemplate.delete(RedisKeys.captcha(captchaKey));

        boolean result = storedCode.equalsIgnoreCase(captchaCode);
        log.debug("验证码校验: key={}, input={}, stored={}, result={}",
            captchaKey, captchaCode, storedCode, result);
        return result;
    }

    /**
     * 生成随机验证码字符串
     */
    private String generateRandomCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CAPTCHA_LENGTH; i++) {
            sb.append(CAPTCHA_CHARS.charAt(random.nextInt(CAPTCHA_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * 生成验证码图片
     */
    private String generateCaptchaImage(String code) {
        BufferedImage image = new BufferedImage(CAPTCHA_WIDTH, CAPTCHA_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        Random random = new Random();

        // 设置抗锯齿
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 填充背景
        g.setColor(new Color(245, 245, 245));
        g.fillRect(0, 0, CAPTCHA_WIDTH, CAPTCHA_HEIGHT);

        // 绘制干扰线
        for (int i = 0; i < 5; i++) {
            g.setColor(new Color(random.nextInt(200), random.nextInt(200), random.nextInt(200)));
            g.drawLine(
                random.nextInt(CAPTCHA_WIDTH), random.nextInt(CAPTCHA_HEIGHT),
                random.nextInt(CAPTCHA_WIDTH), random.nextInt(CAPTCHA_HEIGHT)
            );
        }

        // 绘制干扰点
        for (int i = 0; i < 50; i++) {
            g.setColor(new Color(random.nextInt(200), random.nextInt(200), random.nextInt(200)));
            g.fillOval(random.nextInt(CAPTCHA_WIDTH), random.nextInt(CAPTCHA_HEIGHT), 2, 2);
        }

        // 绘制验证码字符
        g.setFont(new Font("Arial", Font.BOLD, 28));
        for (int i = 0; i < code.length(); i++) {
            g.setColor(new Color(random.nextInt(100), random.nextInt(100), random.nextInt(100)));
            double theta = Math.toRadians(random.nextInt(30) - 15);
            g.rotate(theta, 25 + i * 25, 25);
            g.drawString(String.valueOf(code.charAt(i)), 15 + i * 25, 30);
            g.rotate(-theta, 25 + i * 25, 25);
        }

        g.dispose();

        // 转换为Base64
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            log.error("生成验证码图片失败", e);
            return null;
        }
    }
}
