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
     * <p>v11.36：月度快照缓存——当月已有报告直接返回（零 token）；
     * refresh=true 时强制重新分析并覆盖当月报告。返回不再含 profile（前端独立请求）。
     *
     * @param userId  门户用户 ID
     * @param refresh true=强制重新分析
     * @param range   维度：month=本月（默认，落月度快照）/ 3m / 6m / year（实时计算）
     * @return 报告结构：indicators / incomeSources / debtRisks / suggestions / aiSummary / fromCache / period
     */
    Map<String, Object> analyze(Long userId, boolean refresh, String range);

    /**
     * 历史报告分页（v11.36）
     *
     * @param userId   门户用户 ID
     * @param page     页码（1 起）
     * @param pageSize 每页条数
     * @return { list: [...], total: n }，list 项含 period/healthScore/aiSummary/updateTime
     */
    Map<String, Object> listReports(Long userId, int page, int pageSize);

    /**
     * 获取用户画像（含身份标签字典选项）
     */
    Map<String, Object> getProfile(Long userId);

    /**
     * 更新用户画像（职位/公司/身份标签，与门户共用 portal_user 表）
     */
    Map<String, Object> updateProfile(Long userId, Map<String, String> body);
}