package com.moyun.portal.service.realname;

/**
 * 实名核验渠道抽象
 *
 * <p>当前默认实现为 {@link ManualRealNameVerifier}（人工审核）。
 * 后期接入阿里云 / 腾讯云等实名核验 API 时，新增实现类并将
 * {@code moyun.security.realname-channel} 指向对应渠道即可，
 * 业务代码无需改动。</p>
 *
 * @author moyun
 */
public interface RealNameVerifier {

    /**
     * 渠道标识（落库 verify_channel 字段）
     * <p>manual=人工审核；aliyun/tencent=第三方核验</p>
     */
    String channel();

    /**
     * 执行实名核验
     *
     * @param realName 真实姓名
     * @param certNo   身份证号（明文，仅核验过程中使用，不落日志）
     * @return 核验结果
     */
    RealNameVerifyResult verify(String realName, String certNo);
}
