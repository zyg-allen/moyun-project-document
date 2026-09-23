package com.moyun.ext.job.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.pay.config.PayProperties;
import com.moyun.pay.domain.entity.PayOrder;
import com.moyun.pay.gateway.IPayGateway;
import com.moyun.pay.mapper.PayOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付超时关单定时任务
 *
 * <p>供 Quartz 调度，sys_job.invoke_target 配置：{@code payCloseTimeoutOrderTask.closeTimeoutOrders()}
 * （无参数；超时阈值复用 moyun.pay.order-expire-minutes 配置，默认 30 分钟）。
 *
 * <p>设计要点：
 * <ul>
 *   <li>扫描范围：status=CREATED 且（expire_time 已过期，或 expire_time 为空但创建时间超过阈值）。</li>
 *   <li>主动关单：走 {@link IPayGateway#closeOrder}（先本系统条件更新关单，再尽力通知渠道），
 *       补齐 queryStatus 惰性关单覆盖不到的"下单后再无人轮询"场景。</li>
 *   <li>幂等：网关侧条件更新（仅 CREATED → CLOSED），已支付/已关单的自动跳过并记日志；
 *       并发冲突（0 行更新）同样跳过。</li>
 *   <li>分批扫描：每批 200 条，按 id 游标推进，避免大表全量加载。</li>
 *   <li>非阻断：单笔关单异常不影响整批，记录日志后继续。</li>
 * </ul>
 *
 * @author moyun
 */
@Slf4j
@Component("payCloseTimeoutOrderTask")
public class PayCloseTimeoutOrderTask {

    /** 单批扫描条数 */
    private static final int BATCH_SIZE = 200;

    @Autowired
    private PayOrderMapper payOrderMapper;

    @Autowired
    private IPayGateway payGateway;

    @Autowired
    private PayProperties payProperties;

    /**
     * 扫描并关闭超时未支付订单（关单原因 TIMEOUT）。
     */
    public void closeTimeoutOrders() {
        int timeoutMinutes = payProperties.getOrderExpireMinutes();
        LocalDateTime now = LocalDateTime.now();
        log.info("[pay-close] 超时关单扫描开始，超时阈值 {} 分钟", timeoutMinutes);
        long scanned = 0;
        long closed = 0;
        long skipped = 0;
        Long lastId = 0L;
        while (true) {
            LambdaQueryWrapper<PayOrder> qw = new LambdaQueryWrapper<PayOrder>()
                    .eq(PayOrder::getStatus, PayOrder.STATUS_CREATED)
                    // 过期判定：expire_time 已过；缺失时按创建时间 + 阈值兜底
                    .and(w -> w
                            .lt(PayOrder::getExpireTime, now)
                            .or(x -> x.isNull(PayOrder::getExpireTime)
                                    .lt(PayOrder::getCreateTime, now.minusMinutes(timeoutMinutes))))
                    .gt(PayOrder::getId, lastId)
                    .orderByAsc(PayOrder::getId)
                    .last("LIMIT " + BATCH_SIZE);
            List<PayOrder> batch = payOrderMapper.selectList(qw);
            if (batch == null || batch.isEmpty()) {
                break;
            }
            for (PayOrder order : batch) {
                lastId = order.getId();
                scanned++;
                try {
                    PayOrder result = payGateway.closeOrder(order.getPayNo(), "TIMEOUT");
                    if (result != null && PayOrder.STATUS_CLOSED.equals(result.getStatus())) {
                        closed++;
                    } else {
                        // 扫描到关单间隙被并发处理（已支付/已关单等）→ 幂等跳过
                        skipped++;
                        log.info("[pay-close] 跳过关单 payNo={} 当前状态={}",
                                order.getPayNo(), result == null ? null : result.getStatus());
                    }
                } catch (Exception e) {
                    skipped++;
                    log.error("[pay-close] 关单异常 payNo={} err={}", order.getPayNo(), e.getMessage());
                }
            }
            if (batch.size() < BATCH_SIZE) {
                break;
            }
        }
        log.info("[pay-close] 超时关单扫描结束，扫描 {} 单，关单 {} 单，跳过 {} 单", scanned, closed, skipped);
    }
}
