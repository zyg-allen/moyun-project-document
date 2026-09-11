package com.moyun.ledger.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.ledger.domain.entity.LedgerSavingPlan;
import com.moyun.ledger.domain.entity.LedgerSavingRecord;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 存钱计划服务
 *
 * <p>支持四种存钱方式：
 * 52week=52周存钱法（第n周存10n元，累计13780）；
 * fixed=固定金额分期（periodAmount × 期数）；monthly=每月固定存；custom=自定义递增。
 * 创建计划时按方式自动生成期次流水（ledger_saving_record，全部待存）。
 *
 * @author moyun
 */
public interface ILedgerSavingService extends IService<LedgerSavingPlan> {

    /**
     * 创建存钱计划（按方式生成期次流水）
     *
     * @param userId 门户用户ID
     * @param plan   计划（name/method/targetAmount 必填；fixed/monthly 需 periodAmount；
     *               custom 需 periodCount + increaseStep + 首期金额按 targetAmount/期数折算或 periodAmount）
     * @return 计划ID
     */
    Long createPlan(Long userId, LedgerSavingPlan plan);

    /** 计划列表（status != 0，含汇总：剩余需存/累计存入/目标金额） */
    Map<String, Object> listPlans(Long userId);

    /** 计划详情（计划 + 期次流水列表） */
    Map<String, Object> planDetail(Long userId, Long planId);

    /** 存入某一期（成功：record.status=1，plan.currentAmount 累加；全部完成或达标 → plan.status=2 成功） */
    void deposit(Long userId, Long planId, Long recordId, BigDecimal actualAmount);

    /** 放弃某一期（失败：record.status=2，记录原因） */
    void failRecord(Long userId, Long planId, Long recordId, String reason);

    /** 删除计划（逻辑删 status=0，期次保留） */
    void deletePlan(Long userId, Long planId);

    /** 供其他模块查询用户进行中的计划（暂不使用，预留） */
    List<LedgerSavingRecord> listRecords(Long userId, Long planId);
}