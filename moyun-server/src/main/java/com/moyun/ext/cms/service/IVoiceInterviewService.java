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
}
