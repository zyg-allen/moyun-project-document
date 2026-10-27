package com.moyun.portal.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.core.config.redis.RedisCache;

/**
 * 微信小程序：code2session + 订阅消息下发。
 *
 * <p>未配置 appid/secret/模板 ID 时 {@link #isReady()} 返回 false，
 * 所有调用方应静默跳过（不影响站内通知与邮件）。</p>
 */
@Service
public class WechatMiniProgramService {

    private static final Logger log = LoggerFactory.getLogger(WechatMiniProgramService.class);

    private static final String TOKEN_KEY = "wechat:miniapp:access_token";
    private static final String CODE2SESSION = "https://api.weixin.qq.com/sns/jscode2session";
    private static final String TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";
    private static final String SUBSCRIBE_SEND = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send";

    @Value("${wechat.miniapp.appid:}")
    private String appid;

    @Value("${wechat.miniapp.secret:}")
    private String secret;

    @Value("${wechat.miniapp.memo-template-id:}")
    private String memoTemplateId;

    @Autowired
    private RedisCache redisCache;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    /** 是否具备下发订阅消息的条件 */
    public boolean isReady() {
        return notBlank(appid) && notBlank(secret) && notBlank(memoTemplateId);
    }

    public String getTemplateId() {
        return memoTemplateId;
    }

    /** 小程序登录 code 换取 openid；失败返回 null */
    public String code2Openid(String code) {
        if (!notBlank(appid) || !notBlank(secret) || !notBlank(code)) {
            return null;
        }
        try {
            String url = CODE2SESSION + "?appid=" + appid + "&secret=" + secret
                    + "&js_code=" + code + "&grant_type=authorization_code";
            JsonNode node = getJson(url);
            String openid = node.path("openid").asText(null);
            if (!notBlank(openid)) {
                log.warn("[wx] code2session 失败: errcode={} errmsg={}",
                        node.path("errcode").asInt(), node.path("errmsg").asText());
            }
            return openid;
        } catch (Exception e) {
            log.warn("[wx] code2session 异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 下发待办提醒订阅消息。
     *
     * <p>模板需包含：thing1（事项）、time2（时间）、thing3（备注/重要程度）。</p>
     *
     * @return 是否成功（未配置或用户未订阅时返回 false）
     */
    public boolean sendTodoReminder(String openid, String title, String timeText, String remark) {
        if (!isReady() || !notBlank(openid)) {
            return false;
        }
        String token = accessToken();
        if (token == null) {
            return false;
        }
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("thing1", Map.of("value", cut(title, 20)));
            data.put("time2", Map.of("value", cut(timeText, 20)));
            data.put("thing3", Map.of("value", cut(remark, 20)));

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("touser", openid);
            body.put("template_id", memoTemplateId);
            body.put("page", "pages/mine/memo/index");
            body.put("data", data);
            body.put("miniprogram_state", "formal");
            body.put("lang", "zh_CN");

            HttpRequest req = HttpRequest.newBuilder(URI.create(SUBSCRIBE_SEND + "?access_token=" + token))
                    .timeout(Duration.ofSeconds(8))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
            JsonNode node = mapper.readTree(http.send(req, HttpResponse.BodyHandlers.ofString()).body());
            int errcode = node.path("errcode").asInt(0);
            if (errcode != 0) {
                log.warn("[wx] 订阅消息下发失败 openid={} errcode={} errmsg={}",
                        mask(openid), errcode, node.path("errmsg").asText());
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("[wx] 订阅消息异常: {}", e.getMessage());
            return false;
        }
    }

    private String accessToken() {
        try {
            String cached = redisCache.getCacheObject(TOKEN_KEY);
            if (notBlank(cached)) {
                return cached;
            }
            JsonNode node = getJson(TOKEN_URL + "?grant_type=client_credential&appid=" + appid + "&secret=" + secret);
            String token = node.path("access_token").asText(null);
            if (!notBlank(token)) {
                log.warn("[wx] 获取 access_token 失败: errcode={} errmsg={}",
                        node.path("errcode").asInt(), node.path("errmsg").asText());
                return null;
            }
            redisCache.setCacheObject(TOKEN_KEY, token, 7000, TimeUnit.SECONDS);
            return token;
        } catch (Exception e) {
            log.warn("[wx] 获取 access_token 异常: {}", e.getMessage());
            return null;
        }
    }

    private JsonNode getJson(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8)).GET().build();
        String body = http.send(req, HttpResponse.BodyHandlers.ofString()).body();
        return mapper.readTree(body);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static String cut(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max - 1) + "…";
    }

    private static String mask(String openid) {
        if (openid == null || openid.length() < 8) {
            return "***";
        }
        return openid.substring(0, 4) + "****" + openid.substring(openid.length() - 4);
    }
}
