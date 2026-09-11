package com.moyun.portal.service.realname;

import org.springframework.stereotype.Component;

/**
 * 人工审核渠道（默认）
 *
 * <p>不做自动核验，真实性由后台审核员比对身份证照片与提交信息保障。
 * 核验流水号为空，审核结论以人工审核结果为准。</p>
 *
 * @author moyun
 */
@Component
public class ManualRealNameVerifier implements RealNameVerifier {

    @Override
    public String channel() {
        return "manual";
    }

    @Override
    public RealNameVerifyResult verify(String realName, String certNo) {
        return RealNameVerifyResult.ok(null, "人工审核渠道，真实性由审核员比对证件照片保障");
    }
}
