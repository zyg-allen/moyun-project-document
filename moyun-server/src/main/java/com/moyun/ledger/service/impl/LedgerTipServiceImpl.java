package com.moyun.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.ledger.domain.entity.LedgerTipOrder;
import com.moyun.ledger.mapper.LedgerTipOrderMapper;
import com.moyun.ledger.service.ILedgerTipService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 打赏服务实现（演示：模拟支付成功即落库，不发起真实支付）
 *
 * @author moyun
 */
@Service
public class LedgerTipServiceImpl extends ServiceImpl<LedgerTipOrderMapper, LedgerTipOrder>
        implements ILedgerTipService {

    @Override
    public Long createTip(Long userId, BigDecimal amount, String payWay, String target, String reason) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("请输入有效的赞赏金额");
        }
        if (amount.compareTo(new BigDecimal("999999")) > 0) {
            throw new ServiceException("赞赏金额超出上限");
        }
        LedgerTipOrder order = new LedgerTipOrder();
        order.setUserId(userId);
        order.setAmount(amount);
        order.setPayWay(LedgerTipOrder.PAY_ALIPAY.equals(payWay) ? LedgerTipOrder.PAY_ALIPAY : LedgerTipOrder.PAY_WECHAT);
        order.setTarget(LedgerTipOrder.TARGET_PLATFORM.equals(target) ? LedgerTipOrder.TARGET_PLATFORM : LedgerTipOrder.TARGET_DEVELOPER);
        order.setReason(reason);
        order.setStatus(LedgerTipOrder.STATUS_SUCCESS);
        this.save(order);
        return order.getId();
    }

    @Override
    public BigDecimal totalAmount(Long userId) {
        LambdaQueryWrapper<LedgerTipOrder> q = new LambdaQueryWrapper<>();
        q.eq(LedgerTipOrder::getUserId, userId)
                .eq(LedgerTipOrder::getStatus, LedgerTipOrder.STATUS_SUCCESS);
        List<LedgerTipOrder> orders = this.list(q);
        BigDecimal total = BigDecimal.ZERO;
        for (LedgerTipOrder o : orders) {
            if (o.getAmount() != null) {
                total = total.add(o.getAmount());
            }
        }
        return total;
    }
}