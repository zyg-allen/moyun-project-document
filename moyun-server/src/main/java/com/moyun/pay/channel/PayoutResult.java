package com.moyun.pay.channel;

/**
 * 代付出金受理结果
 *
 * @param success        通道是否受理成功（false → 调用方回滚审核事务）
 * @param channelOrderNo 通道流水号（对账/回溯用；mock 渠道为模拟单号）
 * @param tradeState     通道原始状态（如 SUCCESS / ACCEPTED / FAILED）
 * @param message        失败原因或通道描述（仅记日志，不直接回显前端）
 * @author moyun
 * @since 2026-09-27
 */
public record PayoutResult(boolean success, String channelOrderNo, String tradeState, String message) {

    public static PayoutResult accepted(String channelOrderNo, String tradeState) {
        return new PayoutResult(true, channelOrderNo, tradeState, "受理成功");
    }

    public static PayoutResult failed(String message) {
        return new PayoutResult(false, null, "FAILED", message);
    }
}
