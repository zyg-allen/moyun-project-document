package com.moyun.ledger.service;

import java.util.Map;

/**
 * 记账 AI 财务分析服务接口
 *
 * @author moyun
 */
public interface ILedgerAiAnalysisService {

    /**
     * 生成用户财务分析报告（规则引擎指标 + LLM 综述）
     *
     * @param userId 门户用户 ID
     * @return 报告结构：profile / indicators / incomeSources / debtRisks / suggestions / aiSummary
     */
    Map<String, Object> analyze(Long userId);

    /**
     * 获取用户画像（含身份标签字典选项）
     */
    Map<String, Object> getProfile(Long userId);

    /**
     * 更新用户画像（职位/公司/身份标签，与门户共用 portal_user 表）
     */
    Map<String, Object> updateProfile(Long userId, Map<String, String> body);
}
