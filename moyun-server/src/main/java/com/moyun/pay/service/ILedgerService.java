package com.moyun.pay.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyun.pay.domain.entity.LedgerEntry;

import java.math.BigDecimal;
import java.util.List;

/**
 * 资金/分账流水服务（V11.0 复式记账）
 *
 * <p>金额单位：元（人民币，DECIMAL(18,2)，v11.31 统一）。
 *
 * @author moyun
 */
public interface ILedgerService {

    /**
     * 分账（事务内调用）：平台抽成 + 用户所得 双条流水，金额守恒校验
     *
     * @param payNo   支付单号
     * @param bizType 业务类型
     * @param bizNo   业务单号
     * @param amount  支付金额（元）
     * @param userId  收款用户
     * @param summary 业务摘要前缀（如"打赏收入"）
     * @return 本组流水（含 balanceAfter）
     */
    List<LedgerEntry> settle(String payNo, String bizType, String bizNo, BigDecimal amount,
                             Long userId, String summary);

    /** 用户流水分页（amount/balanceAfter 单位元，恒等映射） */
    IPage<LedgerEntry> myEntries(Long userId, long current, long size);
}
