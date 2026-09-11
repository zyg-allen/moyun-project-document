package com.moyun.pay.channel;

/**
 * 统一支付通知消息（渠道回调验签解析后）
 *
 * @author moyun
 */
public class PayNotifyMessage {

    /** 本系统支付单号 */
    private String payNo;
    /** 三方交易单号 */
    private String channelOrderNo;
    /** 交易状态：SUCCESS / CLOSED / ... */
    private String tradeState;
    /** 三方交易完成时间（yyyy-MM-dd HH:mm:ss） */
    private String successTime;
    /** 三方应答报文（渠道要求的成功应答，如微信 {"code":"SUCCESS"}） */
    private String ackBody;

    public String getPayNo() { return payNo; }
    public void setPayNo(String payNo) { this.payNo = payNo; }
    public String getChannelOrderNo() { return channelOrderNo; }
    public void setChannelOrderNo(String channelOrderNo) { this.channelOrderNo = channelOrderNo; }
    public String getTradeState() { return tradeState; }
    public void setTradeState(String tradeState) { this.tradeState = tradeState; }
    public String getSuccessTime() { return successTime; }
    public void setSuccessTime(String successTime) { this.successTime = successTime; }
    public String getAckBody() { return ackBody; }
    public void setAckBody(String ackBody) { this.ackBody = ackBody; }
}
