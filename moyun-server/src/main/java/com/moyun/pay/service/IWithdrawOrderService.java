package com.moyun.pay.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.pay.domain.entity.WithdrawOrder;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 提现单服务（提现闭环）
 *
 * <p>资金模型：真钱集中于平台公账商户号，虚拟余额为记账；申请时校验可用余额并<b>冻结</b>
 * （可用余额 = balance - frozen_amount，防并发重复申请超提），
 * 审核通过事务内原子扣减冻结（balance/frozen/totalWithdraw 三联动）+ 写 debit 流水 + 置 paying
 * + 代付通道出金（通道未配置时模拟打款并 log.warn），审核驳回解冻回余额可用。
 *
 * @author moyun
 */
public interface IWithdrawOrderService extends IService<WithdrawOrder> {

    /**
     * 用户发起提现（校验绑卡/可用余额，事务：冻结金额 + 落 auditing 单）
     *
     * @return 提现单（含 withdrawNo）
     */
    WithdrawOrder apply(Long userId, BigDecimal amount, Long bankCardId);

    /**
     * 审核通过（事务：auditing→paying 条件更新 → 原子扣减冻结 → 写 debit 流水 → 代付通道打款；
     * 通道未配置时模拟打款成功置 paid）
     */
    void auditPass(Long withdrawId);

    /**
     * 审核驳回（事务：auditing→rejected 条件更新 + 解冻金额回可用余额）
     */
    void auditReject(Long withdrawId, String reason);

    /** 我的提现单分页 */
    IPage<WithdrawOrder> myWithdrawals(Long userId, long current, long size);

    /** 后台提现单分页（status/userId/platformCode 筛选）+ 昵称/银行卡脱敏信息回填 */
    Map<String, Object> adminList(String status, Long userId, String platformCode, long current, long size);

    /** 用户维度汇总（balance/totalIncome/totalWithdraw/审核中金额） */
    Map<String, Object> userSummary(Long userId);
}
