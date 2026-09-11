package com.moyun.pay.controller;

import com.moyun.common.annotation.Anonymous;
import com.moyun.pay.domain.entity.PayNotifyLog;
import com.moyun.pay.gateway.PayGatewayImpl;
import com.moyun.pay.mapper.PayNotifyLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 渠道支付回调入口（V11.0 公共支付通道）
 *
 * <p>@Anonymous 免登录（渠道服务器无法携带用户态），安全由渠道验签保证：
 * 验签失败一律 500 应答，绝不进业务。回调原始报文先落 pay_notify_log 审计。
 *
 * @author moyun
 */
@RestController
@RequestMapping("/pay/callback")
public class PayCallbackController {

    private static final Logger log = LoggerFactory.getLogger(PayCallbackController.class);

    @Autowired
    private PayGatewayImpl payGateway;

    @Autowired
    private PayNotifyLogMapper notifyLogMapper;

    /**
     * 微信支付回调（v3：POST /pay/callback/wechat）
     */
    @Anonymous
    @PostMapping(value = "/wechat", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> wechatNotify(@RequestBody String body,
                                            @RequestHeader Map<String, String> headers) {
        Map<String, String> headerMap = new HashMap<>(headers);
        PayNotifyLog notifyLog = new PayNotifyLog();
        notifyLog.setChannel("wechat");
        notifyLog.setBody(abbreviate(body));
        notifyLog.setCreateTime(LocalDateTime.now());

        Map<String, Object> response = new HashMap<>();
        try {
            payGateway.handleNotify("wechat", headerMap, body);
            notifyLog.setVerifyResult(1);
            notifyLog.setHandleResult(1);
            // 渠道要求成功应答：{"code":"SUCCESS"}
            response.put("code", "SUCCESS");
            response.put("message", "成功");
            return response;
        } catch (Exception e) {
            log.error("[pay-callback] 微信回调处理失败：{}", e.getMessage(), e);
            notifyLog.setVerifyResult(0);
            notifyLog.setHandleResult(0);
            notifyLog.setFailReason(abbreviate(e.getMessage()));
            // 失败应答：渠道将按官方退避策略重试
            response.put("code", "FAIL");
            response.put("message", "处理失败");
            return response;
        } finally {
            try {
                notifyLogMapper.insert(notifyLog);
            } catch (Exception logEx) {
                log.warn("[pay-callback] 审计日志落库失败：{}", logEx.getMessage());
            }
        }
    }

    private String abbreviate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() <= 2000 ? text : text.substring(0, 2000);
    }
}
