package com.moyun.pay.channel;

/**
 * 支付渠道 SPI（V11.0 公共支付通道）
 *
 * <p>实现类按渠道注册（如 wechat），由 {@code PayGatewayImpl} 按 order.channel 路由。
 * 新增渠道（支付宝/云闪付）实现本接口并声明 Bean 即可，业务代码零改动。
 *
 * @author moyun
 */
public interface PayChannel {

    /** 渠道标识（存 pay_order.channel）：wechat / alipay */
    String channelCode();

    /**
     * 预下单
     *
     * @param request 下单请求（payNo/amount 分/subject/expireTime）
     * @return 渠道响应（native 场景返回 codeUrl 供前端渲染二维码）
     */
    PayChannelResponse prepay(PayChannelRequest request);

    /**
     * 渠道侧订单状态查询（主动对账 / mock 模拟支付触发）
     *
     * @param payNo 本系统支付单号
     * @return 渠道响应（paid=true 表示三方已支付成功）
     */
    PayChannelResponse query(String payNo);

    /**
     * 验签回调报文（企业级安全红线：验签失败一律拒绝，绝不做业务处理）
     *
     * @param headers 回调请求头（含签名相关）
     * @param body    回调原始报文
     * @return true=验签通过
     */
    boolean verifyNotify(java.util.Map<String, String> headers, String body);

    /**
     * 解析回调报文为统一通知消息
     *
     * @param body 回调原始报文（验签通过后调用）
     * @return 统一通知消息
     */
    PayNotifyMessage parseNotify(String body);

    /**
     * 渠道侧关单（通知三方不再受理该单；失败不阻断本系统关单）
     *
     * @param payNo 支付单号
     */
    void close(String payNo);
}
