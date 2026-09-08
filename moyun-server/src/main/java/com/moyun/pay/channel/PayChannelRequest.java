package com.moyun.pay.channel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 渠道预下单请求（网关 → 渠道）
 *
 * <p>金额单位：元（人民币，DECIMAL(18,2) 口径）。渠道实现调用三方 API 时自行完成
 * 元→分边界换算（如微信 v3 {@code Amount.total} 要求整数分）。
 *
 * @author moyun
 */
public class PayChannelRequest {

    /** 本系统支付单号（幂等键） */
    private String payNo;
    /** 金额（元） */
    private BigDecimal amount;
    /** 商品描述 */
    private String subject;
    /** 过期时间 */
    private LocalDateTime expireTime;

    public String getPayNo() { return payNo; }
    public void setPayNo(String payNo) { this.payNo = payNo; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }
}
