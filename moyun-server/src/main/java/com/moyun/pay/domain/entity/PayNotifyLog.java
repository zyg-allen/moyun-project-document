package com.moyun.pay.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 渠道回调原始日志（审计留痕）
 *
 * <p>每次渠道回调先落此表（验签前后状态），用于对账与问题回溯，不参与业务流程。
 *
 * <p><b>注意</b>：本实体字段名与 {@code pay_notify_log} 物理列名不一致，
 * 必须靠 {@link TableField} 显式映射（历史命名：列用 snake_case 短语，实体用驼峰语义名）。
 * 缺失映射会导致 insert 报 Unknown column，而调用方 catch 后仅 log.warn，
 * 使回调审计留痕静默全量丢失。
 *
 * @author moyun
 */
@TableName("pay_notify_log")
public class PayNotifyLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 渠道：wechat / alipay → 列 channel */
    private String channel;

    /** 回调报文（截断存储防超长） → 列 raw_body */
    @TableField("raw_body")
    private String body;

    /** 验签结果：1=通过 0=失败 → 列 verify_ok */
    @TableField("verify_ok")
    private Integer verifyResult;

    /** 处理结果：1=成功 0=失败（重放/未知单等） → 列 handled */
    @TableField("handled")
    private Integer handleResult;

    /** 失败原因（截断） → 列 error_msg */
    @TableField("error_msg")
    private String failReason;

    /** 关联支付单（解析成功后回填） → 列 pay_no */
    @TableField("pay_no")
    private String payNo;

    /** 归属端代码（sys_platform.platform_code）：portal / ledger → 列 platform_code */
    private String platformCode;

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
    public String getPlatformCode() { return platformCode; }
    public void setPlatformCode(String platformCode) { this.platformCode = platformCode; }
    public String getPayNo() { return payNo; }
    public void setPayNo(String payNo) { this.payNo = payNo; }
}
