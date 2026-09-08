package com.moyun.pay.channel.wechat;

import com.moyun.pay.channel.PayChannel;
import com.moyun.pay.channel.PayChannelRequest;
import com.moyun.pay.channel.PayChannelResponse;
import com.moyun.pay.channel.PayNotifyMessage;
import com.moyun.pay.config.PayProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 微信支付渠道（V11.0 首个公共支付通道实现）
 *
 * <p><b>当前形态：mock 模拟 + 真实 API TODO 接入点。</b>
 * mock 模式（wechat.mock-enabled=true，默认）下：prepay 生成 codeUrl（weixin://wxpay/mock/...），
 * query 由前端"模拟支付"按钮触发（/portal/pay/mock/{payNo}）置为已支付，回调链路全真演练。
 *
 * <p>生产接入（wechat.mock-enabled=false + 商户参数齐备）：
 * 按 wechatpay-java SDK 实现下列 TODO（native 下单 v3/transactions/native、
 * 回调验签 RSASSA-PKCS1-v1_5 + AES-256-GCM 解密、查单 v3/transactions/out-trade-no）。
 * 参数从 {@link PayProperties.Wechat} 注入，均已保留配置位。
 *
 * @author moyun
 */
@Component
public class WechatPayChannel implements PayChannel {

    private static final Logger log = LoggerFactory.getLogger(WechatPayChannel.class);

    @Autowired
    private PayProperties payProperties;

    @Override
    public String channelCode() {
        return "wechat";
    }

    @Override
    public PayChannelResponse prepay(PayChannelRequest request) {
        PayChannelResponse response = new PayChannelResponse();
        if (payProperties.getWechat().isMockEnabled()) {
            // ===== mock 模式：生成模拟二维码链接，前端收银台渲染为二维码 =====
            response.setPaid(false);
            response.setCodeUrl("weixin://wxpay/mock/" + request.getPayNo());
            response.setTradeState("NOTPAY");
            response.setRawResponse("{\"mock\":true,\"scene\":\"prepay\"}");
            log.info("[wechat-mock] prepay payNo={} amount={}元", request.getPayNo(), request.getAmount());
            return response;
        }

        // ===== 真实 API：native 下单 =====
        // TODO(生产接入 wechatpay-java SDK)：
        //   1. Config config = new RSAAutoCertificateConfig.Builder()
        //          .merchantId(wechat.getMchId())
        //          .privateKeyFromPath(wechat.getPrivateKeyPath())
        //          .merchantSerialNumber(wechat.getMerchantSerial())
        //          .apiV3Key(wechat.getApiV3Key()).build();
        //   2. PrepayRequest apiReq = new PrepayRequest();
        //      apiReq.setAppid(wechat.getAppId()); apiReq.setMchid(wechat.getMchId());
        //      apiReq.setDescription(request.getSubject());
        //      apiReq.setOutTradeNo(request.getPayNo());
        //      apiReq.setNotifyUrl(wechat.getNotifyUrl());
        //      apiReq.setAmount(new Amount().setTotal(yuanToFen(request.getAmount())));  // 边界换算：元→分（微信 v3 要求整数分）
        //      apiReq.setTimeExpire(格式化 request.getExpireTime());
        //   3. PrepayResponse apiResp = new NotificationParser(...).parse(...)
        //      service.post(RequestOption) → codeUrl；
        //      response.setCodeUrl(apiResp.getCodeUrl());
        //   4. 异常按 SDK 模式抛 ServiceException，由网关统一兜底。
        String msg = "微信支付真实 API 未接入：请完成 WechatPayChannel.prepay 中 TODO（wechatpay-java SDK native 下单）";
        log.error("[wechat] {}", msg);
        throw new IllegalStateException(msg);
    }

    @Override
    public PayChannelResponse query(String payNo) {
        PayChannelResponse response = new PayChannelResponse();
        if (payProperties.getWechat().isMockEnabled()) {
            // mock：查询时若模拟器已标记支付（内存标记，由网关 mock 接口写入 pay_order），直接回读
            response.setPaid(false);
            response.setTradeState("NOTPAY");
            response.setRawResponse("{\"mock\":true,\"scene\":\"query\"}");
            return response;
        }

        // TODO(生产接入)：GET v3/transactions/out-trade-no/{payNo}?mchid=...
        //   解析 trade_state（SUCCESS/NOTPAY/CLOSED/...）与 transaction_id 回填 response。
        String msg = "微信支付真实 API 未接入：请完成 WechatPayChannel.query 中 TODO";
        log.error("[wechat] {}", msg);
        throw new IllegalStateException(msg);
    }

    @Override
    public boolean verifyNotify(Map<String, String> headers, String body) {
        if (payProperties.getWechat().isMockEnabled()) {
            // mock：约定头 X-Mock-Signature = sha256(body)，模拟验签通过
            String expect = sha256(body == null ? "" : body);
            String actual = headers == null ? null : headers.get("X-Mock-Signature");
            return expect != null && expect.equalsIgnoreCase(actual);
        }

        // TODO(生产接入)：NotificationParser.parse(...)
        //   1. 用商户私钥 + 平台证书做 RSASSA-PKCS1-v1_5 验签（Wechatpay2Validator）
        //   2. 用 APIv3Key 做 AES-256-GCM 解密 resource.ciphertext
        //   3. 验签失败：记 log.warn 并返回 false（回调入口将回复失败应答，绝不进业务）
        String msg = "微信支付真实 API 未接入：请完成 WechatPayChannel.verifyNotify 中 TODO";
        log.error("[wechat] {}", msg);
        throw new IllegalStateException(msg);
    }

    @Override
    public PayNotifyMessage parseNotify(String body) {
        // mock 报文格式：{"payNo":"...","tradeState":"SUCCESS","channelOrderNo":"mock-txn-..."}
        // 真实报文（v3）为 JSON resource 解密后的结构，TODO 见 verifyNotify
        PayNotifyMessage message = new PayNotifyMessage();
        String payNo = extractJsonField(body, "payNo");
        String tradeState = extractJsonField(body, "tradeState");
        String channelOrderNo = extractJsonField(body, "channelOrderNo");
        String successTime = extractJsonField(body, "successTime");
        message.setPayNo(payNo);
        message.setTradeState(tradeState);
        message.setChannelOrderNo(channelOrderNo);
        message.setSuccessTime(successTime);
        message.setAckBody("{\"code\":\"SUCCESS\",\"message\":\"成功\"}");
        return message;
    }

    @Override
    public void close(String payNo) {
        if (payProperties.getWechat().isMockEnabled()) {
            log.info("[wechat-mock] close payNo={}", payNo);
            return;
        }
        // TODO(生产接入)：POST v3/transactions/out-trade-no/{payNo}/close
    }

    /**
     * 轻量 JSON 字段提取（避免为 mock 引入 Jackson 强依赖；真实接入走 SDK 解析）
     */
    private String extractJsonField(String json, String field) {
        if (json == null || field == null) {
            return null;
        }
        String key = "\"" + field + "\"";
        int idx = json.indexOf(key);
        if (idx < 0) {
            return null;
        }
        int colon = json.indexOf(':', idx + key.length());
        if (colon < 0) {
            return null;
        }
        int start = json.indexOf('"', colon + 1);
        if (start < 0) {
            return null;
        }
        int end = json.indexOf('"', start + 1);
        if (end < 0) {
            return null;
        }
        return json.substring(start + 1, end);
    }

    /**
     * 边界换算：元 → 分（微信支付 v3 API 契约要求整数分）
     * 全链路统一元存储，仅此一处向三方 API 靠拢
     */
    private int yuanToFen(BigDecimal yuan) {
        return yuan.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    private String sha256(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
