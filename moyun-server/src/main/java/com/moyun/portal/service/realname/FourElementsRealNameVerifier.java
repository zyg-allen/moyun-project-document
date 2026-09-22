package com.moyun.portal.service.realname;

import com.moyun.core.config.redis.RedisCache;
import com.moyun.portal.config.RealNameProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 银行卡四要素实名核验（姓名 + 身份证号 + 银行卡号 + 银行预留手机号）
 *
 * <p>完整流程：通道配置检查（未配置明确返回未开通）→ 防重（同一用户每日
 * 核验次数上限，防通道费用滥用）→ 第三方核验（姓名/身份证/卡号/手机号
 * 四要素一致性）→ 结果处理（流水号/结论落结果对象）。
 * 真实第三方核验点以 {@code todo：配置第三方：} 注释标识，接入前
 * 通道层面明确返回"未开通"，不伪装核验通过。
 *
 * <p>安全红线：
 * <ul>
 *   <li>身份证号/银行卡号/手机号仅核验过程内存使用，不落库、日志一律脱敏</li>
 *   <li>核验结果只落结论与流水号，敏感明文不外泄</li>
 * </ul>
 *
 * <p>接入步骤（生产前完成）：
 * <ol>
 *   <li>pom 引入厂商 SDK（阿里云 cloudauth / 腾讯云 faceid 等），
 *       <b>不加依赖前骨架保持注释形态</b></li>
 *   <li>application.yaml 配置 moyun.realname 段（enabled/provider/aliyun.AccessKey）</li>
 *   <li>放开 {@link #verify} 内 {@code todo：配置第三方：} 标记处的 SDK 骨架代码</li>
 * </ol>
 *
 * @author moyun
 */
@Component
public class FourElementsRealNameVerifier {

    private static final Logger log = LoggerFactory.getLogger(FourElementsRealNameVerifier.class);

    /** 四要素核验每日计数 Redis Key（userId 维度，防重复调用通道） */
    private static final String KEY_DAILY_VERIFY = "realname:4elem:daily:%s";

    @Autowired
    private RealNameProperties realNameProperties;

    @Autowired
    private RedisCache redisCache;

    /**
     * 执行四要素核验
     *
     * @param userId     用户 ID（防重计数维度，可为 null 时跳过防重）
     * @param holderName 持卡人姓名
     * @param certNo     身份证号（仅核验过程使用，不落日志）
     * @param cardNo     银行卡号（仅核验过程使用，日志脱敏）
     * @param phone      银行预留手机号（仅核验过程使用，日志脱敏）
     * @return 核验结果：success=true 一致；success=false 不一致或异常；
     *         channelUnavailable=true 通道未开通（降级语义）
     */
    public RealNameVerifyResult verify(Long userId, String holderName, String certNo,
                                       String cardNo, String phone) {
        // 1. 通道配置检查：未配置/占位值明确返回未开通（不静默放行、不伪装通过）
        if (isChannelNotConfigured()) {
            log.warn("[realname-4elem] 实名认证通道未开通（moyun.realname.enabled={} 或 AccessKey 缺失/为 todo 占位值），"
                    + "本次不发起核验 userId={}", realNameProperties.isEnabled(), userId);
            return RealNameVerifyResult.channelUnavailable("实名认证通道未开通，绑定卡将转人工核实");
        }

        // 2. 防重：同一用户每日核验次数上限（第三方通道按次计费，防滥用/防撞库）
        if (userId != null && !acquireDailyQuota(userId)) {
            log.warn("[realname-4elem] 今日核验次数已达上限 userId={} limit={}",
                    userId, realNameProperties.getDailyVerifyLimit());
            return RealNameVerifyResult.fail("今日核验次数已达上限（" + realNameProperties.getDailyVerifyLimit() + " 次），请明日再试");
        }

        try {
            // 3. 第三方四要素核验（请求组装 → 客户端调用 → 响应解析 → 结果处理）
            // todo：配置第三方：实名认证通道（阿里云/腾讯云实名认证API）配置
            // （pom 引入阿里云 cloudauth（com.aliyun:cloudauth20190507 或老版 aliyun-java-sdk-cloudauth）
            //   / 腾讯云 faceid 等 SDK 后，放开以下骨架）
            //
            // --- 请求组装（阿里云银行卡四要素核验 BankCardVerify 示例） ---
            // com.aliyun.cloudauth20190507.Client client = buildClient(); // 按 RealNameProperties.aliyun 初始化，单例复用
            // com.aliyun.cloudauth20190507.models.BankCardVerifyRequest req =
            //         new com.aliyun.cloudauth20190507.models.BankCardVerifyRequest()
            //                 .setName(holderName)      // 姓名
            //                 .setIdentifyNumber(certNo) // 身份证号
            //                 .setBankCardNo(cardNo)     // 银行卡号
            //                 .setMobilePhone(phone);    // 银行预留手机号
            //
            // --- 客户端调用 ---
            // com.aliyun.cloudauth20190507.models.BankCardVerifyResponse resp = client.bankCardVerify(req);
            //
            // --- 响应解析与结果处理 ---
            // // 响应结构：resp.getBody().getData().{ result(核验结论), orderId(流水号) }
            // // result: "T"=四要素一致；"F"=不一致；"P"=通道未确证（转人工）
            // String verifyResult = resp.getBody().getData().getResult();
            // String orderId = resp.getBody().getData().getOrderId();
            // if ("T".equals(verifyResult)) {
            //     log.info("[realname-4elem] 四要素核验一致 userId={} serialNo={} cardNo={}",
            //             userId, orderId, maskCardNo(cardNo));
            //     return RealNameVerifyResult.ok(orderId, "四要素核验一致");
            // }
            // log.warn("[realname-4elem] 四要素核验不一致 userId={} serialNo={} result={} cardNo={}",
            //         userId, orderId, verifyResult, maskCardNo(cardNo));
            // return RealNameVerifyResult.fail("实名信息核验不一致，请核对姓名/证件/卡号/预留手机号");

            // SDK 未接入期间的明确降级（enabled=true 但 SDK 未引入时也不会伪装通过）
            log.warn("[realname-4elem] 实名认证 SDK 未接入（见 todo：配置第三方： 标记），核验未发起 userId={}", userId);
            return RealNameVerifyResult.channelUnavailable("实名认证通道未接入，绑定卡将转人工核实");
        } catch (Exception e) {
            // 4. 异常处理：通道异常不阻断绑卡主流程，降级为失败结论由调用方落 PENDING 转人工
            log.error("[realname-4elem] 核验异常 userId={} err={} cardNo={}",
                    userId, e.getMessage(), maskCardNo(cardNo), e);
            return RealNameVerifyResult.fail("核验服务异常，请稍后重试");
        }
    }

    /**
     * 通道是否未配置：总开关关闭，或 AccessKey 缺失/为 todo 占位值
     */
    private boolean isChannelNotConfigured() {
        if (!realNameProperties.isEnabled()) {
            return true;
        }
        RealNameProperties.Aliyun aliyun = realNameProperties.getAliyun();
        return isBlankOrPlaceholder(aliyun.getAccessKeyId())
                || isBlankOrPlaceholder(aliyun.getAccessKeySecret());
    }

    /**
     * 防重配额：当日首次调用初始化计数（TTL 到次日零点），超出上限拒绝
     *
     * @return true=本次调用获得配额；false=已达上限
     */
    private boolean acquireDailyQuota(Long userId) {
        String dailyKey = String.format(KEY_DAILY_VERIFY, userId);
        Integer count = redisCache.getCacheObject(dailyKey);
        if (count != null && count >= realNameProperties.getDailyVerifyLimit()) {
            return false;
        }
        if (count == null) {
            long secondsToMidnight = Duration.between(
                    LocalDateTime.now(), LocalDate.now().plusDays(1).atStartOfDay()).getSeconds();
            redisCache.setCacheObject(dailyKey, 1, (int) secondsToMidnight, TimeUnit.SECONDS);
        } else {
            redisCache.setCacheObject(dailyKey, count + 1);
        }
        return true;
    }

    private boolean isBlankOrPlaceholder(String s) {
        return s == null || s.isBlank() || s.trim().toLowerCase().startsWith("todo");
    }

    /** 银行卡号脱敏（日志专用，前 4 后 4） */
    private String maskCardNo(String cardNo) {
        if (cardNo == null || cardNo.length() < 10) {
            return "****";
        }
        return cardNo.substring(0, 4) + " **** **** " + cardNo.substring(cardNo.length() - 4);
    }
}
