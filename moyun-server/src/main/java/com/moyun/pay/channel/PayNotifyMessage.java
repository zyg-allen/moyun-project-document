package com.moyun.pay.channel;

import lombok.Getter;

/**
 * 统一支付通知消息（渠道回调验签解析后）
 *
 * @author moyun
 */
@Getter
public class PayNotifyMessage {

    /** 本系统支付单号 */
    private String payNo;
    /** 三方交易单号 */
    private String channelOrderNo;
    /** 交易状态：SUCCESS / CLOSED / ... */
    private String tradeState;
    /** 三方交易完成时间（yyyy-MM-dd HH:mm:ss） */
    private String successTime;
    /**
     * 渠道回传的**实付金额（元）**——用于与本地订单金额比对（见 PayGatewayImpl#handleNotify）。
     *
     * <p>微信 v3 的 {@code amount.total} 单位是**分**，由渠道实现负责归一到"元"（项目金额口径为元）；
     * 渠道未回传金额时为 null，此时不阻断但会告警留痕。</p>
     */
    private java.math.BigDecimal amount;
    /** 三方应答报文（渠道要求的成功应答，如微信 {"code":"SUCCESS"}） */
    private String ackBody;

    public void setPayNo(String payNo) { this.payNo = payNo; }

    public void setChannelOrderNo(String channelOrderNo) { this.channelOrderNo = channelOrderNo; }

    public void setTradeState(String tradeState) { this.tradeState = tradeState; }

    public void setSuccessTime(String successTime) { this.successTime = successTime; }

    public void setAmount(java.math.BigDecimal amount) { this.amount = amount; }

    public void setAckBody(String ackBody) { this.ackBody = ackBody; }
}
