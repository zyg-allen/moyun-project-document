package com.moyun.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 后台运营首页 - 分平台聚合统计 Mapper
 * <p>只承载首页专用的跨模块 COUNT/SUM 聚合（不在 Java 侧拉全表再统计）。
 * 各业务域已有的聚合方法（如 {@code PortalArticleMapper.selectArticleMetrics}、
 * {@code SysAuditTaskMapper.countPendingByType}）由 Service 直接复用，不在此重复。
 * <p>SQL 全部外置 XML：{@code resources/mapper/system/SysDashboardStatsMapper.xml}
 *
 * @author moyun
 */
@Mapper
public interface SysDashboardStatsMapper {

    /**
     * 记账端：交易笔数/今日新增按类型分组
     *
     * @return [{type, cnt, todayCnt}]
     */
    List<Map<String, Object>> selectLedgerTransactionSummary(@Param("todayStart") LocalDateTime todayStart);

    /**
     * 记账端：账户数（资产/负债，仅正常状态）
     *
     * @return [{accountKind, cnt}]
     */
    List<Map<String, Object>> selectLedgerAccountSummary();

    /**
     * 记账端：AI 财务分析报告数
     */
    Long countLedgerAiReports();

    /**
     * 记账端：预算 / 净资产快照数量
     *
     * @return [{itemKey, cnt}]
     */
    Map<String, Object> selectLedgerAuxCounts();

    /**
     * AI 网关：调用总数/成功/失败/Token/花费（全量）
     *
     * @return {total, success, fail, tokens, costYuan}
     */
    Map<String, Object> selectAiExecuteSummary();

    /**
     * AI 网关：今日调用数与失败数
     *
     * @return {todayTotal, todayFail}
     */
    Map<String, Object> selectAiExecuteTodaySummary(@Param("todayStart") LocalDateTime todayStart);

    /**
     * 门户端：AI 语音面试统计（场次/已完成/未完成/平均分/今日新增）
     *
     * @return {total, finished, unfinished, avgScore, todayNew}
     */
    Map<String, Object> selectVoiceInterviewSummary(@Param("todayStart") LocalDateTime todayStart);

    /**
     * 门户端：编程题提交统计（提交数/通过数/今日提交）
     *
     * @return {total, accepted, todayNew}
     */
    Map<String, Object> selectInterviewSubmissionSummary(@Param("todayStart") LocalDateTime todayStart);

    /**
     * 门户端：简历统计（总数/已发布/草稿）
     *
     * @return {total, published, draft}
     */
    Map<String, Object> selectResumeSummary();

    /**
     * 门户端：异步 AI 任务统计（按任务类型或状态分组）
     *
     * @return [{taskType, status, cnt}]
     */
    List<Map<String, Object>> selectPortalAiTaskSummary();

    /**
     * 门户端：成长体系统计（成长记录数/时间线数/当前等级用户数）
     *
     * @return {growthLogs, timeline, leveled}
     */
    Map<String, Object> selectGrowthSummary();

    /**
     * 门户端：内容社区互动统计（评论/话题/专栏/标签）
     *
     * @return [{itemKey, cnt}]
     */
    Map<String, Object> selectContentAuxCounts();

    /**
     * 门户端：近 N 天记账活跃趋势（记账笔数按日）
     *
     * @return [{date, value}]
     */
    List<Map<String, Object>> selectLedgerDailyTrend(@Param("startTime") LocalDateTime startTime);

    /**
     * 门户端：近 N 天 AI 语音面试场次趋势
     *
     * @return [{date, value}]
     */
    List<Map<String, Object>> selectVoiceInterviewDailyTrend(@Param("startTime") LocalDateTime startTime);

    /**
     * 平台端：启用端列表（首页"分平台"骨架）
     * <p>直接读表而非依赖 {@code com.moyun.vip.domain.entity.SysPlatform}，避免
     * system → vip 这条非必要跨模块边（管理端展示平台清单不需要 vip 的实体类型）。
     *
     * @return [{platformCode, platformName, platformType, description, domain, icon, sortOrder}]
     */
    List<Map<String, Object>> selectEnabledPlatforms();

    /**
     * 管理端：后台账号数（未删除）/ 启用角色数
     *
     * @return {sysUsers, sysRoles}
     */
    Map<String, Object> selectAdminAccountSummary();

    /**
     * 管理端：审核任务按状态统计
     *
     * @return {pending, approved, rejected, todayHandled}
     */
    Map<String, Object> selectAuditTaskSummary(@Param("todayStart") LocalDateTime todayStart);
}
