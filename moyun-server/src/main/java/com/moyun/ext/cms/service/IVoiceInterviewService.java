package com.moyun.ext.cms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.core.base.page.PageDomain;
import com.moyun.ext.cms.domain.vo.VoiceInterviewReportVO;
import com.moyun.ext.cms.domain.vo.VoiceInterviewVO;
import com.moyun.ext.cms.domain.vo.VoiceStartConfig;
import com.moyun.portal.domain.entity.PortalVoiceInterview;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 语音面试官 Service 接口（V10.1）
 *
 * <p>核心方法：
 * <ul>
 *   <li>{@link #start} 生成本场题单 + 首问 + greet 话术</li>
 *   <li>{@link #submitAnswer} SSE 双通道流：规则分 + LLM 话术 + nextAction</li>
 *   <li>{@link #requestHint} 调用 HintEngine 分级提示</li>
 *   <li>{@link #forceNext} 强制下一题</li>
 *   <li>{@link #finish} 聚合分数 + 报告</li>
 *   <li>{@link #listMy} / {@link #getDetail} 历史与详情</li>
 * </ul>
 *
 * @author moyun
 */
public interface IVoiceInterviewService {

    /**
     * 开始语音面试：生成本场题单 + 首问 + greet 话术
     */
    VoiceInterviewVO start(Long userId, VoiceStartConfig config);

    /**
     * 提交自我介绍（v11.x 状态机：INTRO_WAITING 阶段）
     * <p>ScoringEngine 4 维度评分存 intro_score_json → 生成追问（INTRO_FOLLOWUP）或进入首题（TECH 系列）
     */
    VoiceInterviewVO submitSelfIntro(Long interviewId, Long userId, String transcript);

    /**
     * 提交答案（SSE 双通道流）
     * <p>事件流：score（规则分）→ speak（LLM 话术）→ data（完整数据）→ end
     *
     * @param interviewId 面试ID
     * @param userId      用户ID
     * @param qaId        问答ID
     * @param transcript  ASR 转写文本
     * @param latencyMs   答题耗时（毫秒）
     */
    SseEmitter submitAnswer(Long interviewId, Long userId, Long qaId, String transcript, Integer latencyMs);

    /**
     * 请求分级提示（调用 HintEngine，hint 使用次数 +1）
     */
    VoiceInterviewVO requestHint(Long interviewId, Long userId, Long qaId);

    /**
     * 强制下一题（用户点"下一题"）
     */
    VoiceInterviewVO forceNext(Long interviewId, Long userId, String reason);

    /**
     * 结束面试：聚合分数 + 报告
     */
    VoiceInterviewReportVO finish(Long interviewId, Long userId);

    /**
     * 我的语音面试列表（分页）
     */
    Page<PortalVoiceInterview> listMy(Long userId, PageDomain pageDomain);

    /**
     * 面试详情（含问答列表）
     */
    VoiceInterviewVO getDetail(Long interviewId, Long userId);

    /**
     * 通过 qaId 查询面试详情（用于错题本桥接，校验 qaId 归属当前用户）
     * <p>返回的 VoiceInterviewVO 中 currentQa 为该 qaId 对应的问答记录
     */
    VoiceInterviewVO getDetailByQaId(Long qaId, Long userId);

    /**
     * v11.x：启用中的岗位模板列表（id/名称/类别/难度），供 portal 开始面试选择
     */
    java.util.List<java.util.Map<String, Object>> listActiveJobTemplates();
    /**
     * v11.30：管理端分页查询所有用户的语音面试（支持 username/position/status 筛选）
     */
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.moyun.portal.domain.entity.PortalVoiceInterview> adminList(
            String username, String position, String status, Integer pageNum, Integer pageSize);

    /**
     * v11.30：管理端查询面试详情（不校验用户归属，含 qaList）
     */
    VoiceInterviewVO adminGetDetail(Long interviewId);

    /**
     * v11.30：管理端删除面试会话（逻辑删除）
     */
    boolean adminDelete(Long interviewId);

    /**
     * v11.30.5：生成/刷新报告分享令牌（仅本人已结束的面试）
     *
     * @param expireDays 分享有效期（天，1-30，默认 7）
     * @return shareToken（前端拼公开链接 /interview/share/{token}）
     */
    String createShareToken(Long interviewId, Long userId, Integer expireDays);

    /**
     * v11.30.5：通过分享令牌公开查看报告（免登录，脱敏不含 userId；过期/不存在返回 null）
     */
    VoiceInterviewReportVO getSharedReport(String shareToken);
}
