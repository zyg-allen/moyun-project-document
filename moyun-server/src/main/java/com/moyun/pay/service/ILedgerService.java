package com.moyun.pay.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyun.pay.domain.entity.LedgerEntry;

import java.math.BigDecimal;
import java.util.List;

/**
 * 资金/分账流水服务（复式记账）
 *
 * <p>金额单位：元（人民币，DECIMAL(18,2)，统一）。
 *
 * <p><b>账户模型分类（新支付场景接入前必读，资金流必须分清）：</b>
 * <ul>
 *   <li><b>平台直收类</b>（用户 → 平台公账，无第三方收款人）：如记账App打赏、面试会员、简历优化、记账VIP。
 *       业务表独立 + bizType 独立 + 回调处理器独立 + {@link #settlePlatform} 单分录
 *       （PLATFORM/credit 全额），不产生用户钱包余额。</li>
 *   <li><b>分账类</b>（用户 → 收款人，平台抽成）：如门户文章打赏、付费阅读、专栏。
 *       业务表独立 + bizType 独立 + 回调处理器独立 + {@link #settle} 双分录
 *       （PLATFORM/credit 抽成 + USER/credit 收款人所得，入钱包余额可提现）。</li>
 *   <li><b>提现出金</b>（用户余额 → 银行卡）：走 WithdrawOrder（USER/debit），不在此分账。</li>
 * </ul>
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

    /**
     * 平台全额入账（事务内调用）：无第三方收款人的业务（如记账App打赏），
     * 支付金额全额计入平台所得，仅写 PLATFORM/credit 单条流水。
     *
     * @param payNo   支付单号
     * @param bizType 业务类型
     * @param bizNo   业务单号
     * @param amount  支付金额（元）
     * @param summary 业务摘要（如"记账App打赏-平台所得"）
     * @return 平台分录
     */
    LedgerEntry settlePlatform(String payNo, String bizType, String bizNo, BigDecimal amount, String summary);

    /** 用户流水分页（amount/balanceAfter 单位元，恒等映射） */
    IPage<LedgerEntry> myEntries(Long userId, long current, long size);
}
