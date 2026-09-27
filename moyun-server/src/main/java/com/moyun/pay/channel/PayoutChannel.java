package com.moyun.pay.channel;

import java.math.BigDecimal;

/**
 * 代付渠道 SPI（提现出金）
 *
 * <p>与 {@link PayChannel}（收款方向）对称：本接口负责**出金方向**。
 * 实现类按渠道注册并由 {@code WithdrawOrderServiceImpl} 选择使用；
 * 真实接入（银行/三方代付）时新增实现并替换配置即可，业务代码零改动。</p>
 *
 * <p><b>金额口径</b>：全链路统一人民币元（{@code BigDecimal}）；
 * 实现类在调用三方 API 时自行完成边界换算（多数代付通道要求整数分）。</p>
 *
 * <p><b>幂等契约</b>：{@code withdrawNo} 即通道侧幂等键（out_biz_no）。
 * 实现必须保证同一 {@code withdrawNo} 重复提交不会重复出金——
 * 这是本系统"审核通过可能被重试"的前提。</p>
 *
 * <p><b>结果语义</b>：
 * <ul>
 *   <li>受理成功 → 返回 {@link PayoutResult#success}，单据保持 {@code paying}，
 *       由通道异步回执/对账推进 {@code paid}；</li>
 *   <li>明确失败 → 返回 {@link PayoutResult#failed}，调用方回滚整个审核事务；</li>
 *   <li>受理未知（超时）→ 调用方应走查单核对，**禁止盲目重发**。</li>
 * </ul>
 *
 * @author moyun
 * @since 2026-09-27
 */
public interface PayoutChannel {

    /** 渠道标识：mock / bank / alipay 等 */
    String channelCode();

    /**
     * 发起代付出金
     *
     * @param withdrawNo  本系统提现单号（通道幂等键）
     * @param amount      出金金额（元）
     * @param bankCardId  收款银行卡ID（实现内部解密取号，禁止落日志）
     * @param userId      提现用户（对账维度）
     * @return 受理结果；{@link PayoutResult#success}=false 时调用方回滚
     */
    PayoutResult pay(String withdrawNo, BigDecimal amount, Long bankCardId, Long userId);
}
