package com.moyun.pay.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 支付站内通知（V11.0）
 *
 * @author moyun
 */
@TableName("pay_notification")
public class PayNotification {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 通知类型：pay(支付结果) / withdraw(提现进度) / account(账户变动) */
    private String notifyType;

    /** 关联单号（支付单/提现单） */
    private String refNo;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 已读：0=未读 1=已读 */
    private Integer readFlag;

    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getNotifyType() { return notifyType; }
    public void setNotifyType(String notifyType) { this.notifyType = notifyType; }
    public String getRefNo() { return refNo; }
    public void setRefNo(String refNo) { this.refNo = refNo; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getReadFlag() { return readFlag; }
    public void setReadFlag(Integer readFlag) { this.readFlag = readFlag; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
