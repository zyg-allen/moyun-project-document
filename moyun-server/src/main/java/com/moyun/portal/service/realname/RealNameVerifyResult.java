package com.moyun.portal.service.realname;

import lombok.Data;

/**
 * 实名核验结果
 *
 * <p>{@code channelUnavailable=true} 表示通道未配置/未开通（非核验不通过），
 * 调用方应按降级处理（如银行卡落 PENDING 转人工），而非判定用户信息有误。
 *
 * @author moyun
 */
@Data
public class RealNameVerifyResult {

    /** 核验是否通过（manual 渠道恒为 true，由人工审核兜底） */
    private boolean success;

    /** 通道未配置/未开通（true=本次未发起真实核验，走降级） */
    private boolean channelUnavailable;

    /** 核验流水号（第三方渠道返回，用于对账与申诉） */
    private String serialNo;

    /** 结果说明 */
    private String message;

    public static RealNameVerifyResult ok(String serialNo, String message) {
        RealNameVerifyResult r = new RealNameVerifyResult();
        r.setSuccess(true);
        r.setSerialNo(serialNo);
        r.setMessage(message);
        return r;
    }

    public static RealNameVerifyResult fail(String message) {
        RealNameVerifyResult r = new RealNameVerifyResult();
        r.setSuccess(false);
        r.setMessage(message);
        return r;
    }

    /** 通道未配置/未开通（明确降级语义，区别于核验不通过） */
    public static RealNameVerifyResult channelUnavailable(String message) {
        RealNameVerifyResult r = new RealNameVerifyResult();
        r.setSuccess(false);
        r.setChannelUnavailable(true);
        r.setMessage(message);
        return r;
    }
}
