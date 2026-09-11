package com.moyun.pay.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 渠道回调原始日志（V11.0 审计留痕）
 *
 * <p>每次渠道回调先落此表（验签前后状态），用于对账与问题回溯，不参与业务流程。
 *
 * @author moyun
 */
@TableName("pay_notify_log")
public class PayNotifyLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 渠道：wechat / alipay */
    private String channel;

    /** 回调报文（截断存储防超长） */
    private String body;

    /** 验签结果：1=通过 0=失败 */
    private Integer verifyResult;

    /** 处理结果：1=成功 0=失败（重放/未知单等） */
    private Integer handleResult;

    /** 失败原因（截断） */
    private String failReason;

    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Integer getVerifyResult() { return verifyResult; }
    public void setVerifyResult(Integer verifyResult) { this.verifyResult = verifyResult; }
    public Integer getHandleResult() { return handleResult; }
    public void setHandleResult(Integer handleResult) { this.handleResult = handleResult; }
    public String getFailReason() { return failReason; }
    public void setFailReason(String failReason) { this.failReason = failReason; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
