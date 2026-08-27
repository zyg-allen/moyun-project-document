package com.moyun.portal.controller;

import com.moyun.common.annotation.RateLimiter;
import com.moyun.common.constant.HttpStatus;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.page.PageDomain;
import com.moyun.ext.cms.domain.vo.VoiceInterviewReportVO;
import com.moyun.ext.cms.domain.vo.VoiceInterviewVO;
import com.moyun.ext.cms.domain.vo.VoiceStartConfig;
import com.moyun.ext.cms.service.IVoiceInterviewService;
import com.moyun.ext.cms.service.IWrongQuestionService;
import com.moyun.portal.util.PortalSecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;



/**
 * 语音面试官 Controller（V10.1 MVP）
 *
 * <p>7 接口：start / submitAnswer(SSE) / requestHint / forceNext / finish / listMy / detail
 * <p>路径：/portal/interview/voice/*
 * <p>⚠️ 严格使用 {@link PortalSecurityUtils#getUserId()}，不能用 SecurityUtils.getUserId()
 *
 * @author moyun
 */
@Tag(name = "语音面试官", description = "V10.1 语音面试官 MVP 接口")
@RestController
@RequestMapping("/portal/interview/voice")
@Validated
public class PortalVoiceInterviewController extends BaseController {

    @Autowired
    private IVoiceInterviewService voiceInterviewService;

    @Autowired
    private IWrongQuestionService wrongQuestionService;

    private Long currentUserId() {
        return PortalSecurityUtils.getUserId();
    }

    /**
     * 1. 开始语音面试
     * <p>生成本场题单 + 首问 + greet 话术
     */
    @Operation(summary = "开始语音面试", description = "按岗位/场景/画像抽取5题，生成首问与开场话术")
    @PostMapping("/start")
    @RateLimiter(key = "voice:start", time = 86400, count = 5)
    public AjaxResult start(@Valid @RequestBody VoiceStartConfig config) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(voiceInterviewService.start(userId, config));
    }

    /**
     * 2. 提交答案（SSE 双通道流）
     * <p>事件流：score（规则分）→ speak（LLM话术）→ data（完整数据）→ end
     */
    @Operation(summary = "提交答案", description = "SSE 流式返回规则分与LLM话术")
    @PostMapping(value = "/{id:[0-9]+}/answer", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    @RateLimiter(key = "voice:answer", time = 3600, count = 60)
    public SseEmitter answer(@PathVariable("id") Long id, @RequestBody AnswerRequest body) {
        Long userId = currentUserId();
        if (userId == null) {
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event().name("error").data("登录已过期"));
                emitter.complete();
            } catch (Exception ignored) {
            }
            return emitter;
        }
        if (body == null || body.getQaId() == null || body.getTranscript() == null) {
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event().name("error").data("qaId 与 transcript 不能为空"));
                emitter.complete();
            } catch (Exception ignored) {
            }
            return emitter;
        }
        return voiceInterviewService.submitAnswer(id, userId, body.getQaId(), body.getTranscript(), body.getLatencyMs());
    }

    /**
     * 3. 请求分级提示
     */
    @Operation(summary = "请求提示", description = "调用 HintEngine 生成分级提示，hint 使用次数+1")
    @PostMapping("/{id:[0-9]+}/hint")
    @RateLimiter(key = "voice:hint", time = 3600, count = 30)
    public AjaxResult hint(@PathVariable("id") Long id, @RequestBody HintRequest body) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        if (body == null || body.getQaId() == null) {
            return AjaxResult.error("qaId 不能为空");
        }
        return AjaxResult.success(voiceInterviewService.requestHint(id, userId, body.getQaId()));
    }

    /**
     * 4. 强制下一题
     */
    @Operation(summary = "强制下一题", description = "用户点'下一题'，跳过追问直接推进")
    @PostMapping("/{id:[0-9]+}/next")
    @RateLimiter(key = "voice:next", time = 3600, count = 30)
    public AjaxResult next(@PathVariable("id") Long id, @RequestBody(required = false) NextRequest body) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        String reason = body == null ? "user_skip" : body.getReason();
        return AjaxResult.success(voiceInterviewService.forceNext(id, userId, reason));
    }

    /**
     * 5. 结束面试
     */
    @Operation(summary = "结束面试", description = "聚合分数 + 生成报告（维度/亮点/薄弱点/逐题点评）")
    @PostMapping("/{id:[0-9]+}/finish")
    public AjaxResult finish(@PathVariable("id") Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        VoiceInterviewReportVO report = voiceInterviewService.finish(id, userId);
        return AjaxResult.success(report);
    }

    /**
     * 6. 我的语音面试列表
     */
    @Operation(summary = "我的语音面试列表", description = "分页查询当前用户的语音面试历史")
    @GetMapping("/my/list")
    public AjaxResult myList(PageDomain query) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(voiceInterviewService.listMy(userId, query));
    }

    /**
     * 7. 面试详情
     */
    @Operation(summary = "面试详情", description = "查询面试会话详情（含问答列表与当前题目）")
    @GetMapping("/{id:[0-9]+}")
    public AjaxResult detail(@PathVariable("id") Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        VoiceInterviewVO vo = voiceInterviewService.getDetail(id, userId);
        return AjaxResult.success(vo);
    }

    /**
     * 8. 薄弱题一键加入错题本
     * <p>通过 qaId 回查 questionId，调用 IWrongQuestionService.recordWrongQuestion（幂等）
     */
    @Operation(summary = "薄弱题加入错题本", description = "通过语音面试问答ID关联题目加入错题本（幂等）")
    @PostMapping("/qa/{qaId}/toWrongBook")
    public AjaxResult toWrongBook(@PathVariable("qaId") Long qaId) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        VoiceInterviewVO vo = voiceInterviewService.getDetailByQaId(qaId, userId);
        if (vo == null || vo.getCurrentQa() == null || vo.getCurrentQa().getQuestionId() == null) {
            return AjaxResult.error("无法关联题目，加入错题本失败");
        }
        Long questionId = vo.getCurrentQa().getQuestionId();
        Long wrongId = wrongQuestionService.recordWrongQuestion(userId, questionId, qaId);
        return AjaxResult.success(wrongId);
    }

    /** 提交答案请求体 */
    @lombok.Data
    public static class AnswerRequest {
        /** 问答ID */
        private Long qaId;
        /** ASR 转写文本 */
        private String transcript;
        /** 答题耗时（毫秒） */
        private Integer latencyMs;
    }

    /** 提示请求体 */
    @lombok.Data
    public static class HintRequest {
        /** 问答ID */
        private Long qaId;
    }

    /** 下一题请求体 */
    @lombok.Data
    public static class NextRequest {
        /** 原因（user_skip/stuck/manual） */
        private String reason;
    }
}
