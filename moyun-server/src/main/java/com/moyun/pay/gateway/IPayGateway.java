package com.moyun.pay.gateway;

import com.moyun.pay.channel.PayChannelRequest;
import com.moyun.pay.channel.PayChannelResponse;
import com.moyun.pay.domain.entity.PayOrder;

import java.math.BigDecimal;

import java.util.List;
import java.util.Map;

/**
 * 统一支付网关（V11.0 公共支付通道对外唯一门面）
 *
 * <p>业务方（打赏/会员/课程...）只感知本接口与 bizType，不感知任何渠道细节。
 * 状态机、幂等、事务分发、分账推进全部在实现类收口。
 *
 * @author moyun
 */
public interface IPayGateway {

    /**
     * 统一下单（幂等：同 bizType+bizNo 的 CREATED 单复用）
     *
     * @param bizType 业务类型（tip/member/course/...）
     * @param bizNo   业务单号（业务方本地单 ID）
     * @param channel 支付渠道（wechat）
     * @param amount  金额（元）
     * @param subject 商品描述
     * @return 支付单（含 codeUrl / payNo / expireTime）
     */
    PayOrder createOrder(String bizType, String bizNo, String channel, BigDecimal amount, String subject);

    /**
     * 支付单状态查询（前端收银台轮询用）
     *
     * @param payNo 支付单号
     * @return 支付单
     */
    PayOrder queryStatus(String payNo);

    /**
     * 手动/超时关单（幂等；先本系统关单，再尽力通知渠道）
     *
     * @param payNo   支付单号
     * @param reason  关单原因：TIMEOUT / ADMIN_MANUAL_CLOSE
     * @return 关单后的支付单
     */
    PayOrder closeOrder(String payNo, String reason);

    /**
     * 按支付单号查询（服务层内部使用）
     */
    PayOrder getByPayNo(String payNo);

    /**
     * mock 模式：模拟渠道支付成功（触发与真实回调完全相同的后续链路）
     *
     * @return 模拟支付结果（含最新状态）
     */
    Map<String, Object> mockPaySuccess(String payNo);
}
