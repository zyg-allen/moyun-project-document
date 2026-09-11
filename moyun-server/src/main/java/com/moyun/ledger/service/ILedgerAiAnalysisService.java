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
     * 提交异步分析任务（v11.55）
     *
     * <p>LLM 生成长（快速模型 30-60s+），同步等待体验差。提交后立即返回 taskId，
     * 前端轮询 {@link #getAnalysisTask}。同用户已有进行中任务时直接返回该任务（防重复烧 token）。
     *
     * @param userId 门户用户 ID
     * @param range  维度：month/3m/6m/year（非法回落 month）
     * @return { taskId, status: pending|running, resubmitted: 是否复用进行中任务 }
     */
    Map<String, Object> submitAnalysisTask(Long userId, String range);

    /**
     * 查询异步任务状态（v11.55）
     *
     * @param userId  门户用户 ID（归属校验，非本人任务返回 not_found）
     * @param taskId  任务 ID
     * @return { status: pending|running|success|failed|not_found, report(完成时), error(失败时) }
     */
    Map<String, Object> getAnalysisTask(Long userId, String taskId);

    /**
     * 历史报告分页（v11.36；v11.55 起同一 period 可多版本，按时间倒序）
     *
     * @param userId   门户用户 ID
     * @param page     页码（1 起）
     * @param pageSize 每页条数
     * @return { list: [...], total: n }，list 项含 id/period/healthScore/aiSummary/updateTime
     */
    Map<String, Object> listReports(Long userId, int page, int pageSize);

    /**
     * 报告详情（v11.55 历史版本完整回看）
     *
     * @param userId   门户用户 ID（归属校验）
     * @param reportId 报告 ID
     * @return 与 analyze 相同结构的完整报告（fromCache=true）
     */
    Map<String, Object> getReportDetail(Long userId, Long reportId);

    /**
     * 删除报告版本（v11.55 用户可清理不满意的历史版本）
     *
     * @param userId   门户用户 ID（归属校验）
     * @param reportId 报告 ID
     */
    void deleteReport(Long userId, Long reportId);

    /**
     * 获取用户画像（含身份标签字典选项）
     */
    Map<String, Object> getProfile(Long userId);

    /**
     * 更新用户画像（职位/公司/身份标签，与门户共用 portal_user 表）
     */
    Map<String, Object> updateProfile(Long userId, Map<String, String> body);
}
