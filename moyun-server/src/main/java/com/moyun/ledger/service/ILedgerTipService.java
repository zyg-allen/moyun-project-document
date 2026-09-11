package com.moyun.ledger.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.ledger.domain.entity.LedgerTipOrder;

import java.math.BigDecimal;

/**
 * 打赏服务（演示性质：模拟支付成功即落库）
 *
 * @author moyun
 */
public interface ILedgerTipService extends IService<LedgerTipOrder> {

    /** 发起打赏（模拟支付成功，直接落库） */
    Long createTip(Long userId, BigDecimal amount, String payWay, String target, String reason);

    /** 累计打赏金额（status=1） */
    BigDecimal totalAmount(Long userId);
}