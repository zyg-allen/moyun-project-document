package com.moyun.portal.service.realname;

import lombok.Data;

/**
 * 实名核验结果
 *
 * @author moyun
 */
@Data
public class RealNameVerifyResult {

    /** 核验是否通过（manual 渠道恒为 true，由人工审核兜底） */
    private boolean success;

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
}
