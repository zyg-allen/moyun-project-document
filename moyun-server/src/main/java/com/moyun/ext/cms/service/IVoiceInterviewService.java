package com.moyun.ext.cms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.core.base.page.PageDomain;
import com.moyun.ext.cms.domain.vo.VoiceInterviewReportVO;
import com.moyun.ext.cms.domain.vo.VoiceInterviewVO;
import com.moyun.ext.cms.domain.vo.VoiceStartConfig;
import com.moyun.portal.domain.entity.PortalVoiceInterview;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * 语音面试官 Service 接口（V3：统一 AI 入口 · 纯 agent 自由面试）
 *
 * <p>核心方法：
 * <ul>
 *   <li>{@link #start} 创建会话 + agent 开场白首问 + 滑窗初始化</li>
 *   <li>{@link #submitAnswer} SSE 流式轮次：delta（面试官话术增量）→ end（nextQaId/finished）</li>
 *   <li>{@link #requestHint} 面试官 agent 滑窗提示</li>
 *   <li>{@link #finish} 收口会话 + 异步批量分析</li>
 *   <li>{@link #listMy} / {@link #getDetail} 历史与详情</li>
 * </ul>
 *
 * @author moyun
 */
public interface IVoiceInterviewService {

    /**
     * 开始语音面试：创建会话 + agent 开场白首问 + 滑窗初始化
     */
    VoiceInterviewVO start(Long userId, VoiceStartConfig config);

    /**
     * 提交答案（SSE 流式轮次）
     * <p>事件流：delta（面试官话术增量，打字机+分句TTS）→ end（roundDone/nextQaId/nextQuestion/finished）
     *
     * @param interviewId 面试ID
     * @param userId      用户ID
     * @param qaId        问答ID
     * @param transcript  ASR 转写文本
     * @param latencyMs   答题耗时（毫秒）
     * @param skip        true=候选人跳过本题（transcript 可为空，滑窗注入跳过标记）
     */
    SseEmitter submitAnswer(Long interviewId, Long userId, Long qaId, String transcript, Integer latencyMs, Boolean skip);

    /**
     * 请求思考提示（面试官 agent 基于滑窗上下文生成一句引导，hint 使用次数 +1）
     */
    VoiceInterviewVO requestHint(Long interviewId, Long userId, Long qaId);

    /**
     * 结束面试：聚合分数 + 报告
     * <p>V2：同步段仅收口会话状态并触发异步批量分析（返回报告骨架），
     * 前端轮询 {@link #getAnalysisStatus(Long, Long)} 至 analysis_status=2 后拉取完整报告。
     */
    VoiceInterviewReportVO finish(Long interviewId, Long userId);

    /**
     * V2：查询报告分析状态（前端进度条轮询）
     *
     * @return key: analysisStatus(0未分析/1分析中/2已完成) / analysisProgress(0-100)
     */
    Map<String, Object> getAnalysisStatus(Long interviewId, Long userId);

    /**
     * 重新生成报告——已结束面试重置分析状态后复用异步批量分析链路
     * （逐题补分析 + 聚合 + 整场 LLM 复盘）。前端轮询 {@link #getAnalysisStatus(Long, Long)}
     * 至 analysis_status=2 后拉取完整报告。
     */
    VoiceInterviewReportVO regenerateReport(Long interviewId, Long userId);

    /**
     * 我的语音面试列表（分页）
     */
    Page<PortalVoiceInterview> listMy(Long userId, PageDomain pageDomain);

    /**
     * 面试详情（含问答列表）
     */
    VoiceInterviewVO getDetail(Long interviewId, Long userId);

    /**
     * 断点续接：查询当前用户最近一个进行中的会话（意外关闭后恢复提示用）
     *
     * @return 空 Map 表示无进行中会话；否则 key: interviewId / position / scene / startTime /
     *         answered(已答题数) / totalQa / elapsedSec(中断前已用时长，秒)
     */
    java.util.Map<String, Object> getActiveInterview(Long userId);

    /**
     * 断点续接：恢复进行中会话（校验归属，记录 resume 事件，返回恢复快照含 qaList + currentQa）
     */
    VoiceInterviewVO resumeInterview(Long interviewId, Long userId);

    /**
     * 通过 qaId 查询面试详情（用于错题本桥接，校验 qaId 归属当前用户）
     * <p>返回的 VoiceInterviewVO 中 currentQa 为该 qaId 对应的问答记录
     */
    VoiceInterviewVO getDetailByQaId(Long qaId, Long userId);

    /**
     * 管理端分页查询所有用户的语音面试（支持 username/position/status 筛选）
     */
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.moyun.portal.domain.entity.PortalVoiceInterview> adminList(
            String username, String position, String status, Integer pageNum, Integer pageSize);

    /**
     * 管理端查询面试详情（不校验用户归属，含 qaList）
     */
    VoiceInterviewVO adminGetDetail(Long interviewId);

    /**
     * 管理端删除面试会话（逻辑删除）
     */
    boolean adminDelete(Long interviewId);

    /**
     * 生成/刷新报告分享令牌（仅本人已结束的面试）
     *
     * @param expireDays 分享有效期（天，1-30，默认 7）
     * @return shareToken（前端拼公开链接 /interview/share/{token}）
     */
    String createShareToken(Long interviewId, Long userId, Integer expireDays);

    /**
     * 通过分享令牌公开查看报告（免登录，脱敏不含 userId；过期/不存在返回 null）
     */
    VoiceInterviewReportVO getSharedReport(String shareToken);
}
