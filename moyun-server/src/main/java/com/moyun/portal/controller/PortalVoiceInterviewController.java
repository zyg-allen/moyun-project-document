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
import com.moyun.ext.cms.service.VoiceAsrService;
import com.moyun.portal.service.PortalFreeTrialService;
import com.moyun.portal.util.PortalSecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;



/**
 * 语音面试官 Controller（MVP）
 *
 * <p>核心链路：start（创建会话+agent 首问） / submitAnswer(SSE 流式轮次) / requestHint / finish / listMy / detail
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

    @Autowired
    private VoiceAsrService voiceAsrService;

    @Autowired
    private com.moyun.ext.cms.service.interview.HintEngine hintEngine;

    @Autowired
    private com.moyun.portal.mapper.PortalInterviewQuestionMapper questionMapper;

    /** 面试会员校验（语音面试为会员付费点） */
    @Autowired
    private PortalInterviewVipController interviewVipController;

    /** 免费体验次数服务（非会员每场景 2 次） */
    @Autowired
    private com.moyun.portal.service.PortalFreeTrialService freeTrialService;

    private Long currentUserId() {
        return PortalSecurityUtils.getUserId();
    }

    /**
     * 1. 开始语音面试
     * <p>创建会话 + agent 开场白首问 + 滑窗记忆初始化
     * <p>面试会员付费点落地——会员不限次；非会员可免费体验 2 次
     * （portal_free_trial 场景 voice_interview，原子消耗），用完返回 402 引导开通
     */
    @Operation(summary = "开始语音面试", description = "创建会话并生成 agent 开场白首问（会员不限次，非会员免费体验2次）")
    @PostMapping("/start")
    @RateLimiter(key = "voice:start", time = 3600, count = 20)
    public AjaxResult start(@Valid @RequestBody VoiceStartConfig config) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        // 会员 或 免费体验未用完（每用户 2 次）
        if (!interviewVipController.isVip(userId)) {
            if (!freeTrialService.tryConsume(userId, PortalFreeTrialService.SCENE_VOICE_INTERVIEW)) {
                return AjaxResult.error(402, "免费体验次数已用完，语音面试为面试会员专属功能，请先开通面试会员");
            }
        }
        return AjaxResult.success(voiceInterviewService.start(userId, config));
    }

    /**
     * 2. 提交答案（SSE 流式轮次）
     * <p>事件流：delta（面试官话术增量）→ end（roundDone/nextQaId/nextQuestion/finished）
     */
    @Operation(summary = "提交答案", description = "SSE 流式返回面试官话术（delta 打字机 + end 轮次推进）；skip=true 跳过本题")
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
        boolean skip = body != null && Boolean.TRUE.equals(body.getSkip());
        if (body == null || body.getQaId() == null || (!skip && body.getTranscript() == null)) {
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event().name("error").data("qaId 与 transcript 不能为空"));
                emitter.complete();
            } catch (Exception ignored) {
            }
            return emitter;
        }
        return voiceInterviewService.submitAnswer(id, userId, body.getQaId(), body.getTranscript(),
                body.getLatencyMs(), skip);
    }

    /**
     * 3. 请求思考提示（V3：面试官 agent 基于滑窗上下文生成一句引导）
     */
    @Operation(summary = "请求提示", description = "面试官 agent 生成一句思考提示（不泄露答案），hint 使用次数+1")
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
     * 4. 结束面试
     * <p>V2：同步段仅收口会话状态并触发异步批量分析（返回报告骨架）；
     * 前端轮询 5.1 分析状态接口，analysisStatus=2 后拉取完整报告。
     */
    @Operation(summary = "结束面试", description = "收口会话并触发异步批量分析；轮询 analysis 接口获取进度")
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
     * 5.1 报告分析状态（V2：前端进度条轮询）
     */
    @Operation(summary = "报告分析状态", description = "analysisStatus(0未分析/1分析中/2已完成) + analysisProgress(0-100)")
    @GetMapping("/{id:[0-9]+}/analysis")
    public AjaxResult analysisStatus(@PathVariable("id") Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(voiceInterviewService.getAnalysisStatus(id, userId));
    }

    /**
     * 5.2 重新生成报告
     * <p>已结束面试重置分析状态后重跑异步批量分析链路（逐题补分析 + 聚合 + 整场 LLM 复盘）；
     * 前端轮询 5.1 分析状态接口，analysisStatus=2 后拉取完整报告。
     */
    @Operation(summary = "重新生成报告", description = "已结束面试重跑逐题分析+整场 LLM 复盘；轮询 analysis 接口获取进度")
    @PostMapping("/{id:[0-9]+}/regenerate-report")
    @RateLimiter(key = "voice:regenerate", time = 3600, count = 10)
    public AjaxResult regenerateReport(@PathVariable("id") Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        VoiceInterviewReportVO report = voiceInterviewService.regenerateReport(id, userId);
        return AjaxResult.success(report);
    }

    /**
     * 5.5 生成报告分享令牌
     */
    @Operation(summary = "生成报告分享令牌", description = "仅本人已结束的面试可分享；有效期 1-30 天，默认 7 天")
    @PostMapping("/{id:[0-9]+}/share")
    @RateLimiter(key = "voice:share", time = 3600, count = 20)
    public AjaxResult share(@PathVariable("id") Long id,
                            @RequestParam(required = false) Integer expireDays) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        try {
            String token = voiceInterviewService.createShareToken(id, userId, expireDays);
            return AjaxResult.success(token);
        } catch (com.moyun.common.exception.system.ServiceException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 5.6 通过分享令牌查看报告（免登录公开接口）
     */
    @Operation(summary = "分享报告查看", description = "通过令牌公开查看面试报告（脱敏，不含用户信息；过期返回错误）")
    @GetMapping("/share/{token}")
    public AjaxResult sharedReport(@PathVariable("token") String token) {
        VoiceInterviewReportVO report = voiceInterviewService.getSharedReport(token);
        if (report == null) {
            return AjaxResult.error("分享链接不存在或已过期");
        }
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
     * 6.9 查询进行中会话（断点续接：意外关闭后再次进入，提示可继续）
     */
    @Operation(summary = "查询进行中会话", description = "返回最近一个未结束的面试（interviewId/answered/totalQa/elapsedSec），空表示无")
    @GetMapping("/active")
    public AjaxResult active() {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(voiceInterviewService.getActiveInterview(userId));
    }

    /**
     * 6.10 恢复进行中会话（断点续接）
     * <p>返回恢复快照（qaList 历史问答 + currentQa 待答题），前端据此重建面试页
     */
    @Operation(summary = "恢复进行中会话", description = "断点续接：返回含历史问答与当前题的恢复快照")
    @GetMapping("/{id:[0-9]+}/resume")
    public AjaxResult resume(@PathVariable("id") Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(voiceInterviewService.resumeInterview(id, userId));
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

    /**
     * 9. 语音转文字（ASR 兜底）
     * <p>浏览器 Web Speech API 不可用（国内网络/Firefox 等）时，前端 MediaRecorder
     * 录音上传，服务端调用 DashScope 转写返回文本。
     */
    @Operation(summary = "语音转文字", description = "上传录音（WAV），DashScope ASR 转写为文本")
    @PostMapping("/asr")
    @RateLimiter(key = "voice:asr", time = 3600, count = 200)
    public AjaxResult asr(@RequestParam("audio") MultipartFile audio) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        if (audio == null || audio.isEmpty()) {
            return AjaxResult.error("音频文件不能为空");
        }
        try {
            String transcript = voiceAsrService.transcribe(audio);
            return AjaxResult.success(Map.of("transcript", transcript));
        } catch (IllegalStateException e) {
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            logger.error("[ASR] 语音转写接口失败", e);
            return AjaxResult.error("语音识别失败，请重试或手动输入");
        }
    }

    /**
     * 11a. 按题目 ID 生成分级提示（补建：语音演示页 useInterviewHint 调用）
     */
    @Operation(summary = "按题目ID请求提示", description = "HintEngine 规则版分级提示（1~3 级），无需面试会话")
    @GetMapping("/hint")
    @RateLimiter(key = "voice:hint", time = 3600, count = 60)
    public AjaxResult hintByQuestion(@RequestParam("questionId") Long questionId,
                                     @RequestParam(value = "level", defaultValue = "1") Integer level) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        com.moyun.portal.domain.entity.PortalInterviewQuestion question = questionMapper.selectById(questionId);
        if (question == null || "2".equals(String.valueOf(question.getDelFlag()))) {
            return AjaxResult.error("题目不存在");
        }
        return AjaxResult.success(hintEngine.generateHint(question, Math.max(1, Math.min(3, level))));
    }

    /**
     * 11b. 按题目 ID 提取关键词（补建：语音演示页 useInterviewHint 调用）
     */
    @Operation(summary = "按题目ID提取关键词", description = "从题目 tags+solution 提取关键词（最多 12 个）")
    @GetMapping("/keywords")
    @RateLimiter(key = "voice:hint", time = 3600, count = 60)
    public AjaxResult keywordsByQuestion(@RequestParam("questionId") Long questionId) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        com.moyun.portal.domain.entity.PortalInterviewQuestion question = questionMapper.selectById(questionId);
        if (question == null || "2".equals(String.valueOf(question.getDelFlag()))) {
            return AjaxResult.error("题目不存在");
        }
        return AjaxResult.success(hintEngine.generateKeywords(question));
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
        /** V3：跳过本题（true 时 transcript 可为空，滑窗注入跳过标记） */
        private Boolean skip;
    }

    /** 提示请求体 */
    @lombok.Data
    public static class HintRequest {
        /** 问答ID */
        private Long qaId;
    }
}
