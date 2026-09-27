package com.moyun.pay.channel.mock;

import com.moyun.pay.channel.PayoutChannel;
import com.moyun.pay.channel.PayoutResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 模拟代付渠道（提现出金联调用）
 *
 * <p><b>用途</b>：在未接入真实代付通道（银行/三方）时，让"申请 → 审核通过 → 出金 → 已打款"
 * 整条提现链路可端到端联调。与 {@code MockSmsSender} 同思路：**逻辑按真实走，只有出金这一步是模拟**。</p>
 *
 * <p><b>与历史实现的区别</b>：历史 {@code WithdrawOrderServiceImpl.payout()} 内置了
 * "未配置通道即模拟打款成功"的分支——那是**隐式降级**（生产漏配也会走到假打款）。
 * 现改为显式渠道 Bean：</p>
 * <ul>
 *   <li>本类仅在 {@code moyun.pay.payout.mock-enabled=true} 时装配 → 联调环境走它；</li>
 *   <li>真实通道实现（接入后新增）在 {@code mock-enabled=false} 时装配；</li>
 *   <li>两者都不满足（未接入且 mock 关闭）→ **无 Bean → 审核通过时明确拒绝**，不会假打款。</li>
 * </ul>
 *
 * <p><b>诚实性</b>：日志显式标注"模拟出金、资金未实际划出"，避免运维误判已真实打款。</p>
 *
 * @author moyun
 * @since 2026-09-27
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "moyun.pay.payout", name = "mock-enabled",
        havingValue = "true", matchIfMissing = true)
public class MockPayoutChannel implements PayoutChannel {

    /** 模拟通道流水号序列（仅用于构造可读单号） */
    private static final AtomicLong SEQ = new AtomicLong(1);

    @Override
    public String channelCode() {
        return "mock";
    }

    @Override
    public PayoutResult pay(String withdrawNo, BigDecimal amount, Long bankCardId, Long userId) {
        // 模拟通道流水号：mockpayout-{时间戳}-{序号}，便于与真实通道单号区分
        String channelOrderNo = "mockpayout-" + System.currentTimeMillis() + "-" + SEQ.getAndIncrement();
        log.warn("[payout-mock] 模拟出金成功（资金未实际划出，仅联调）：withdrawNo={}, userId={}, amount={}元, bankCardId={}, channelOrderNo={}",
                withdrawNo, userId, amount, bankCardId, channelOrderNo);
        // tradeState 用通道语义值：受理成功（真实通道通常异步回执推进终态）
        return PayoutResult.accepted(channelOrderNo, "SUCCESS");
    }
}
