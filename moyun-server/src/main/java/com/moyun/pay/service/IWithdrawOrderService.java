package com.moyun.pay.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.pay.domain.entity.WithdrawOrder;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 提现单服务（提现闭环）
 *
 * <p>资金模型：真钱集中于平台公账商户号，虚拟余额为记账；发起时仅校验余额不扣款，
 * 审核通过时原子扣减（防超扣）+ 写资金流水 + 商户号出金（预留真实打款调用点），
 * 审核驳回则单据关闭、余额不动。
 *
 * @author moyun
 */
public interface IWithdrawOrderService extends IService<WithdrawOrder> {

    /**
     * 用户发起提现（校验余额/绑卡，落 auditing 单；不扣款）
     *
     * @return 提现单（含 withdrawNo）
     */
    WithdrawOrder apply(Long userId, BigDecimal amount, Long bankCardId);

    /**
     * 审核通过（事务：原子扣减余额 → 置 paid → 写 debit 流水 → 预留出金调用点）
     */
    void auditPass(Long withdrawId);

    /**
     * 审核驳回（置 rejected + 原因；余额不动）
     */
    void auditReject(Long withdrawId, String reason);

    /** 我的提现单分页 */
    IPage<WithdrawOrder> myWithdrawals(Long userId, long current, long size);

    /** 后台提现单分页（status/userId 筛选）+ 昵称/银行卡脱敏信息回填 */
    Map<String, Object> adminList(String status, Long userId, long current, long size);

    /** 用户维度汇总（balance/totalIncome/totalWithdraw/审核中金额） */
    Map<String, Object> userSummary(Long userId);
}
