package com.moyun.ledger.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.ledger.domain.entity.LedgerTipOrder;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 记账App打赏服务（接入公共支付通道）
 *
 * <p>链路：校验（金额/幂等）→ 落 pending 单 → payGateway 统一下单
 * (bizType=ledger_tip, platformCode=ledger) → 返回收银台参数（payNo/codeUrl/...）
 * → 收银台扫码或 mock 模拟支付 → 网关回调置 paid + 平台全额分账（LedgerTipPayCallbackHandler）。
 *
 * @author moyun
 */
public interface ILedgerTipService extends IService<LedgerTipOrder> {

    /**
     * 发起打赏下单（公共通道完整链路）
     *
     * <p>校验：金额 0.01~10000 元、scale≤2、clientUuid 幂等防重复提交；
     * 幂等：同 clientUuid 已有 paid 单拒绝重复，pending 未过期单复用（网关同单复用 codeUrl）。
     *
     * @return 收银台参数：payNo/codeUrl/amount/expireTime/tipOrderId/mockEnabled
     */
    Map<String, Object> createTipOrder(Long userId, BigDecimal amount, String payChannel,
                                       String target, String reason, String clientUuid);

    /** 累计打赏金额（status=paid，SQL SUM） */
    BigDecimal totalAmount(Long userId);

    /** 我的赞赏记录（分页，含 pending/paid） */
    Page<LedgerTipOrder> myTips(Long userId, long current, long size);
}
