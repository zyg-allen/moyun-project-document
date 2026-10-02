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
 * 微信支付渠道（首个公共支付通道实现）
 *
 * <p><b>当前形态：mock 模拟 + 真实 API 完整骨架（注释）。</b>
 * <ol>
 *   <li>mock 模式（wechat.mock-enabled=true）：prepay 生成 codeUrl（weixin://wxpay/mock/...），
 *       query 由前端"模拟支付"按钮触发（/portal/pay/mock/{payNo}）置为已支付，回调链路全真演练。</li>
 *   <li><b>未配置商户参数（mock-enabled=false 但 appId/mchId/merchantSerial/privateKeyPath/apiV3Key/notifyUrl
 *       任一缺失或仍为 todo- 占位）：四处入口（prepay / query / close / verifyNotify）一律
 *       fail-closed 明确拒绝，绝不静默降级为 mock。</b></li>
 *   <li>商户参数齐备：进入真实 API 分支。因工程按规范未引入 wechatpay-java SDK 依赖
 *       （pom 无 com.github.wechatpay-apiv3:wechatpay-java），真实调用以完整注释骨架给出
 *       （SDK 调用组装 / 参数构造 / 响应解析 / 异常处理），实际发起调用的位置统一标注
 *       {@code todo：配置第三方：}；接入时引入依赖、放开注释骨架即可运行。</li>
 * </ol>
 *
 * <p>生产接入（wechatpay-java SDK）按本类内 5 处骨架注释执行：
 * native 下单 v3/transactions/native、查单 v3/transactions/out-trade-no、
 * 回调验签 RSASSA-PKCS1-v1_5 + AES-256-GCM 解密（NotificationParser）、关单 v3/transactions/out-trade-no/{no}/close。
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
        PayProperties.Wechat wechat = payProperties.getWechat();
        // mock 分支仅当显式开启 mock-enabled 时进入；真实模式参数缺失一律 fail-closed。
        // 参数缺失时若静默降级为 mock，prepay 会返回假二维码并让网关落库 CREATED 单；
        // 且订单按 mock 建、回调按真实模式拒绝，链路自相矛盾 —— 故四处（prepay/query/close/verifyNotify）统一 fail-closed。
        if (wechat.isMockEnabled()) {
            // ===== mock 模式：生成模拟二维码链接，前端收银台渲染为二维码 =====
            response.setPaid(false);
            response.setCodeUrl("weixin://wxpay/mock/" + request.getPayNo());
            response.setTradeState("NOTPAY");
            response.setRawResponse("{\"mock\":true,\"scene\":\"prepay\"}");
            log.info("[wechat-mock] prepay payNo={} amount={}元", request.getPayNo(), request.getAmount());
            return response;
        }
        if (!isConfigured(wechat)) {
            throw new IllegalStateException(realModeNotConfigured("prepay"));
        }

        // ===== 真实 API：native 下单（POST v3/transactions/native） =====
        // todo：配置第三方：微信支付商户号/APIv3密钥/商户证书序列号配置后启用真实调用
        // 依赖说明：需在 pom 引入 com.github.wechatpay-apiv3:wechatpay-java（当前按规范未引入，不新增依赖）
        //
        // ---- 完整调用骨架（接入时放开注释并删除下方 throw）----
        // // 0. 商户配置（应用级单例，启动时构建一次缓存复用；RSA 密钥/平台证书加载昂贵，严禁每次请求重建）
        // RSAAutoCertificateConfig config = new RSAAutoCertificateConfig.Builder()
        //         .merchantId(wechat.getMchId())
        //         .privateKeyFromPath(wechat.getPrivateKeyPath())
        //         .merchantSerialNumber(wechat.getMerchantSerial())
        //         .apiV3Key(wechat.getApiV3Key())
        //         .build();
        // NativePayService service = new NativePayService.Builder().config(config).build();
        // // 1. 参数构造（元 → 分边界换算：微信 v3 契约要求整数分）
        // PrepayRequest apiReq = new PrepayRequest();
        // apiReq.setAppid(wechat.getAppId());
        // apiReq.setMchid(wechat.getMchId());
        // apiReq.setDescription(request.getSubject());
        // apiReq.setOutTradeNo(request.getPayNo());                       // 幂等键：本系统支付单号
        // apiReq.setNotifyUrl(wechat.getNotifyUrl());                     // 公网回调地址
        // apiReq.setTimeExpire(java.time.format.DateTimeFormatter
        //         .ofPattern("yyyyMMddHHmmss").format(request.getExpireTime())); // 订单过期时刻
        // PrepayRequest.Amount amount = new PrepayRequest.Amount();
        // amount.setTotal(yuanToFen(request.getAmount()));
        // apiReq.setAmount(amount);
        // // 2. 发起调用 + 响应解析
        // PrepayResponse apiResp = service.prepay(apiReq);
        // response.setPaid(false);
        // response.setCodeUrl(apiResp.getCodeUrl());                      // native 二维码链接
        // response.setTradeState("NOTPAY");
        // response.setRawResponse(apiResp.toString());
        // return response;
        // // 3. 异常处理：ServiceException（渠道业务失败，含 code/message）与 HttpException（网络层）
        // //    直接向上抛出，由网关 createOrder 统一兜底记录；本系统不落 CREATED 单（调用成功才落库）。
        throw new IllegalStateException("微信支付真实 API 未启用：请引入 wechatpay-java 依赖并放开 WechatPayChannel.prepay 注释骨架");
    }

    @Override
    public PayChannelResponse query(String payNo) {
        PayChannelResponse response = new PayChannelResponse();
        PayProperties.Wechat wechat = payProperties.getWechat();
        if (wechat.isMockEnabled()) {
            // mock：本地无支付语义，状态由 /portal/pay/mock/{payNo} 模拟器写入 pay_order，
            // 网关 queryStatus 直接回读库表；渠道侧恒回 NOTPAY
            response.setPaid(false);
            response.setTradeState("NOTPAY");
            response.setRawResponse("{\"mock\":true,\"scene\":\"query\"}");
            return response;
        }
        if (!isConfigured(wechat)) {
            // fail-closed：不允许在"真实模式未配置"时假装查单成功（恒 NOTPAY 会让调用方
            // 误以为渠道确认了未支付状态）
            throw new IllegalStateException(realModeNotConfigured("query"));
        }

        // ===== 真实 API：查单（GET v3/transactions/out-trade-no/{outTradeNo}?mchid={mchId}） =====
        // todo：配置第三方：微信支付商户号/APIv3密钥/商户证书序列号配置后启用真实调用
        //
        // ---- 完整调用骨架（接入时放开注释并删除下方 throw）----
        // QueryOrderByOutTradeNoRequest apiReq = new QueryOrderByOutTradeNoRequest();
        // apiReq.setOutTradeNo(payNo);
        // apiReq.setMchid(wechat.getMchId());
        // Transaction tx = service.queryOrderByOutTradeNo(apiReq);        // service 复用 prepay 的单例
        // // 响应解析：trade_state 状态机映射
        // //   SUCCESS → paid=true；NOTPAY/USERPAYING/ACCEPT → paid=false（处理中，等待回调/继续轮询）
        // //   CLOSED/REVOKED/PAYERROR → paid=false（终态，网关将按 CLOSED 处理并关单）
        // response.setPaid("SUCCESS".equals(tx.getTradeState()));
        // response.setTradeState(tx.getTradeState());
        // response.setChannelOrderNo(tx.getTransactionId());
        // response.setRawResponse(tx.toString());
        // return response;
        // // 异常处理：查单失败（网络/系统错误）向上抛出，由网关 queryStatus 捕获后容忍降级（仅记日志，等待回调驱动）
        throw new IllegalStateException("微信支付真实 API 未启用：请引入 wechatpay-java 依赖并放开 WechatPayChannel.query 注释骨架");
    }

    @Override
    public boolean verifyNotify(Map<String, String> headers, String body) {
        PayProperties.Wechat wechat = payProperties.getWechat();
        // mock 分支：仅当显式开启 mock-enabled 时进入（否则静默降级会让验签退化为自算 sha256(body)，可被攻击者伪造回调）。
        if (wechat.isMockEnabled()) {
            // mock：约定头 X-Mock-Signature = hex(HMAC-SHA256(mock-signature-secret, body))
            //
            // 原实现是 sha256(body) —— 无密钥，等于"验签"只是完整性校验，
            // 任何能构造 body 的人都能伪造回调把订单置为已支付（清单 #12）。
            String secret = wechat.getMockSignatureSecret();
            if (secret == null || secret.isBlank()) {
                // 未配置密钥：fail-closed 拒绝，并给出可操作的配置指引（不做静默降级）
                log.error("[wechat-mock] 未配置 moyun.pay.wechat.mock-signature-secret，"
                        + "mock 回调验签无法进行，已拒绝本次回调。请在 application-dev.yaml 或环境变量"
                        + " PAY_MOCK_SIGNATURE_SECRET 中配置密钥后重试。");
                return false;
            }
            String expect = hmacSha256Hex(secret, body == null ? "" : body);
            String actual = headers == null ? null : headers.get("X-Mock-Signature");
            // 恒时比较：避免通过响应时间侧信道逐字节猜签名
            return expect != null && actual != null
                    && java.security.MessageDigest.isEqual(
                            expect.getBytes(StandardCharsets.UTF_8),
                            actual.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
        }

        // 真实模式：商户参数缺失属部署错误 → fail-closed 明确拒绝，绝不降级
        if (!isConfigured(wechat)) {
            String reason = realModeNotConfigured("verifyNotify");
            log.error(reason);
            return false;
        }

        // ===== 真实 API：回调验签 + 报文解密（NotificationParser） =====
        // todo：配置第三方：微信支付商户号/APIv3密钥/商户证书序列号配置后启用真实调用
        //
        // ---- 完整调用骨架（接入时放开注释并删除下方 throw）----
        // // 1. 组装验签请求参数（从回调 headers/body 提取）
        // //    Wechatpay-Timestamp / Wechatpay-Nonce / Wechatpay-Signature / Wechatpay-Serial（平台证书序列号）
        // RequestParam requestParam = new RequestParam.Builder()
        //         .serialNo(headers.get("Wechatpay-Serial"))
        //         .nonce(headers.get("Wechatpay-Nonce"))
        //         .signature(headers.get("Wechatpay-Signature"))
        //         .timestamp(headers.get("Wechatpay-Timestamp"))
        //         .body(body)
        //         .build();
        // // 2. NotificationParser：内部完成 RSASSA-PKCS1-v1_5 验签（平台证书公钥）
        // //    + APIv3Key AES-256-GCM 解密 resource.ciphertext，一步得到解密后的交易对象
        // NotificationParser parser = new NotificationParser(config);    // config 复用 prepay 的单例
        // Transaction tx = parser.parse(requestParam, Transaction.class);
        // // 3. 验签失败的异常处理（安全红线：验签失败一律 false，绝不进业务）
        // //    try { parser.parse(...) } catch (ValidationException e) { log.warn(...); return false; }
        // // 4. SPI 形态适配：verify 与 parse 分离，将解密后的报文经 ThreadLocal 传递给 parseNotify
        // //    DECRYPTED_NOTIFY_BODY.set(JSONUtil.toJsonStr(tx));
        // return true;
        throw new IllegalStateException("微信支付真实 API 未启用：请引入 wechatpay-java 依赖并放开 WechatPayChannel.verifyNotify 注释骨架");
    }

    @Override
    public PayNotifyMessage parseNotify(String body) {
        // 兼容两种报文（字段级回退提取）：
        //   mock：{"payNo":"...","tradeState":"SUCCESS","channelOrderNo":"mock-txn-..."}
        //   微信 v3 验签解密后的交易对象：{"out_trade_no":"...","trade_state":"SUCCESS","transaction_id":"...","success_time":"..."}
        PayNotifyMessage message = new PayNotifyMessage();
        message.setPayNo(firstNonNull(extractJsonField(body, "payNo"), extractJsonField(body, "out_trade_no")));
        message.setTradeState(firstNonNull(extractJsonField(body, "tradeState"), extractJsonField(body, "trade_state")));
        message.setChannelOrderNo(firstNonNull(extractJsonField(body, "channelOrderNo"), extractJsonField(body, "transaction_id")));
        message.setSuccessTime(firstNonNull(extractJsonField(body, "successTime"), extractJsonField(body, "success_time")));
        // 金额（用于与本地订单金额比对）：
        //   · 扁平 "amount"（元）—— mock / 自定义报文
        //   · 微信 v3 "amount":{"total":100,...} —— total 单位是**分**，movePointLeft(2) 归一到元
        java.math.BigDecimal amountYuan = extractJsonNumber(body, "amount");
        if (amountYuan == null) {
            java.math.BigDecimal totalFen = extractJsonNumber(body, "total");
            if (totalFen != null) {
                amountYuan = totalFen.movePointLeft(2);
            }
        }
        message.setAmount(amountYuan);
        message.setAckBody("{\"code\":\"SUCCESS\",\"message\":\"成功\"}");
        return message;
    }

    @Override
    public void close(String payNo) {
        PayProperties.Wechat wechat = payProperties.getWechat();
        if (wechat.isMockEnabled()) {
            log.info("[wechat-mock] close payNo={}", payNo);
            return;
        }
        if (!isConfigured(wechat)) {
            // fail-closed：不允许假装关单成功。调用方 closeOrderInternal 已对该异常做
            // "仅记日志不阻断"处理（本地关单已先行完成），故抛出能让日志如实反映渠道未关单。
            throw new IllegalStateException(realModeNotConfigured("close"));
        }

        // ===== 真实 API：关单（POST v3/transactions/out-trade-no/{outTradeNo}/close） =====
        // todo：配置第三方：微信支付商户号/APIv3密钥/商户证书序列号配置后启用真实调用
        //
        // ---- 完整调用骨架（接入时放开注释并删除下方 throw）----
        // CloseOrderRequest apiReq = new CloseOrderRequest();
        // apiReq.setOutTradeNo(payNo);
        // apiReq.setMchid(wechat.getMchId());
        // service.closeOrder(apiReq);
        // // 异常处理（幂等容忍）：
        // //   - ORDER_CLOSED（渠道已关）→ 视为成功，记 info 返回
        // //   - ORDER_PAID（渠道已支付，不可关）→ 记 warn 并向上抛出，由网关触发查单补单（防止丢单）
        // //   - 网络类异常 → 向上抛出，网关已先行本地关单（尽力通知语义，失败不阻断）
        throw new IllegalStateException("微信支付真实 API 未启用：请引入 wechatpay-java 依赖并放开 WechatPayChannel.close 注释骨架");
    }

    /**
     * 商户参数齐备性检查：appId/mchId/merchantSerial/privateKeyPath/apiV3Key/notifyUrl
     * 全部非空且非 todo- 占位值才算已配置
     */
    private boolean isConfigured(PayProperties.Wechat wechat) {
        return notPlaceholder(wechat.getAppId())
                && notPlaceholder(wechat.getMchId())
                && notPlaceholder(wechat.getMerchantSerial())
                && notPlaceholder(wechat.getPrivateKeyPath())
                && notPlaceholder(wechat.getApiV3Key())
                && notPlaceholder(wechat.getNotifyUrl());
    }

    private boolean notPlaceholder(String value) {
        return value != null && !value.isBlank() && !value.startsWith("todo");
    }

    /**
     * 真实模式但商户参数未配置时的统一拒绝原因（fail-closed）。
     *
     * <p>prepay / query / close / verifyNotify 四处共用同一口径：**mock 关闭且商户参数缺失
     * 即视为部署错误并显式拒绝**，绝不静默降级为 mock 模拟逻辑。</p>
     *
     * <p>保留 {@code todo：配置第三方：} 标记，便于沿用既有的配置位检索约定。</p>
     */
    private String realModeNotConfigured(String scene) {
        return "[wechat] 商户参数未配置，真实模式已拒绝执行 " + scene + "（fail-closed）。"
                + "请检查 appId/mchId/merchantSerial/privateKeyPath/apiV3Key/notifyUrl 六项；"
                + "todo：配置第三方：微信支付商户号/APIv3密钥/商户证书序列号配置后启用真实调用";
    }

    private String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }

    /**
     * 提取**数值**字段（{@link #extractJsonField} 只取带引号的字符串，读不了金额数字）。
     *
     * <p>兼容两种写法：{@code "amount": 1.00}（裸数字）与 {@code "amount":"1.00"}（字符串）。
     * 若该 key 后面跟的是对象/数组（如微信 {@code "amount":{"total":100}}），返回 null，
     * 由调用方改用嵌套子键（{@code total}）读取。</p>
     */
    private java.math.BigDecimal extractJsonNumber(String json, String field) {
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
        int i = colon + 1;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
            i++;
        }
        if (i < json.length() && json.charAt(i) == '"') {
            i++;
        }
        int start = i;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (Character.isDigit(c) || c == '.' || c == '-') {
                i++;
            } else {
                break;
            }
        }
        if (i == start) {
            return null;
        }
        try {
            return new java.math.BigDecimal(json.substring(start, i));
        } catch (NumberFormatException e) {
            return null;
        }
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

    /**
     * HMAC-SHA256 十六进制小写摘要（mock 回调验签用）。
     *
     * @param secret 服务端密钥（非空）
     * @param body   原始报文体
     */
    private String hmacSha256Hex(String secret, String body) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 不可用", e);
        }
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
