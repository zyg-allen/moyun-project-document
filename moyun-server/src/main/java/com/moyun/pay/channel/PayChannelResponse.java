package com.moyun.pay.channel;

/**
 * 渠道响应（渠道 → 网关）
 *
 * @author moyun
 */
public class PayChannelResponse {

    /** 三方是否已支付成功 */
    private boolean paid;
    /** 微信 native 支付二维码链接 */
    private String codeUrl;
    /** 三方交易单号（微信 transaction_id） */
    private String channelOrderNo;
    /** 三方交易状态（微信 trade_state） */
    private String tradeState;
    /** 渠道原始返回（排障用，不落业务表） */
    private String rawResponse;

    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }
    public String getCodeUrl() { return codeUrl; }
    public void setCodeUrl(String codeUrl) { this.codeUrl = codeUrl; }
    public String getChannelOrderNo() { return channelOrderNo; }
    public void setChannelOrderNo(String channelOrderNo) { this.channelOrderNo = channelOrderNo; }
    public String getTradeState() { return tradeState; }
    public void setTradeState(String tradeState) { this.tradeState = tradeState; }
    public String getRawResponse() { return rawResponse; }
    public void setRawResponse(String rawResponse) { this.rawResponse = rawResponse; }
}
