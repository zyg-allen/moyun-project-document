package com.moyun.pay.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 统一支付单（V11.0 公共支付通道）
 *
 * <p>状态机：CREATED(待支付) → PAID(已支付) → SETTLED(已分账)
 *            CREATED → CLOSED(已关闭, 超时/手动)；PAID/SETTLED 为终态
 *
 * <p>金额单位：元（人民币，DECIMAL(18,2)，v11.31 统一；原 BIGINT 分方案已废止）
 *
 * @author moyun
 */
@TableName("pay_order")
public class PayOrder {

    /** 状态：待支付 */
    public static final String STATUS_CREATED = "CREATED";
    /** 状态：已支付（资金到账，待分账） */
    public static final String STATUS_PAID = "PAID";
    /** 状态：已分账（终态） */
    public static final String STATUS_SETTLED = "SETTLED";
    /** 状态：已关闭（终态） */
    public static final String STATUS_CLOSED = "CLOSED";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 支付单号（全局唯一，幂等键）：PAY + yyyyMMddHHmmss + 6位随机 */
    private String payNo;

    /** 业务类型：tip(打赏)，后续 member/course/... 复用 */
    private String bizType;

    /** 业务单号（如打赏单 ID） */
    private String bizNo;

    /** 支付渠道：wechat / alipay */
    private String channel;

    /** 金额（元） */
    private BigDecimal amount;

    /** 商品描述 */
    private String subject;

    /** 支付单状态：CREATED / PAID / SETTLED / CLOSED */
    private String status;

    /** 渠道二维码链接（native 场景） */
    private String codeUrl;

    /** 三方交易单号 */
    private String channelOrderNo;

    /** 三方交易状态 */
    private String tradeState;

    /** 过期时间 */
    private LocalDateTime expireTime;

    /** 支付成功时间 */
    private LocalDateTime paySuccessTime;

    /** 分账完成时间 */
    private LocalDateTime settleTime;

    /** 关单原因：TIMEOUT / ADMIN_MANUAL_CLOSE */
    private String closeReason;

    /** 关单时间 */
    private LocalDateTime closedTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPayNo() { return payNo; }
    public void setPayNo(String payNo) { this.payNo = payNo; }
    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public String getBizNo() { return bizNo; }
    public void setBizNo(String bizNo) { this.bizNo = bizNo; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCodeUrl() { return codeUrl; }
    public void setCodeUrl(String codeUrl) { this.codeUrl = codeUrl; }
    public String getChannelOrderNo() { return channelOrderNo; }
    public void setChannelOrderNo(String channelOrderNo) { this.channelOrderNo = channelOrderNo; }
    public String getTradeState() { return tradeState; }
    public void setTradeState(String tradeState) { this.tradeState = tradeState; }
    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }
    public LocalDateTime getPaySuccessTime() { return paySuccessTime; }
    public void setPaySuccessTime(LocalDateTime paySuccessTime) { this.paySuccessTime = paySuccessTime; }
    public LocalDateTime getSettleTime() { return settleTime; }
    public void setSettleTime(LocalDateTime settleTime) { this.settleTime = settleTime; }
    public String getCloseReason() { return closeReason; }
    public void setCloseReason(String closeReason) { this.closeReason = closeReason; }
    public LocalDateTime getClosedTime() { return closedTime; }
    public void setClosedTime(LocalDateTime closedTime) { this.closedTime = closedTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
