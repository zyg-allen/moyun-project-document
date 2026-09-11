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
import com.moyun.ext.cms.service.interview.InterviewAgentClient;
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

    @Autowired
    private VoiceAsrService voiceAsrService;

    @Autowired
    private InterviewAgentClient agentClient;

    @Autowired
    private com.moyun.ext.cms.service.interview.HintEngine hintEngine;

    @Autowired
    private com.moyun.portal.mapper.PortalInterviewQuestionMapper questionMapper;

    private Long currentUserId() {
        return PortalSecurityUtils.getUserId();
    }

    /**
     * 1. 开始语音面试
     * <p>生成本场题单 + 首问 + greet 话术
     */
    @Operation(summary = "开始语音面试", description = "按岗位/场景/画像抽取5题，生成首问与开场话术")
    @PostMapping("/start")
    @RateLimiter(key = "voice:start", time = 3600, count = 20)
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
     * 5.5 生成报告分享令牌（v11.30.5）
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
     * 5.6 通过分享令牌查看报告（免登录公开接口，v11.30.5）
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
     * 10. 可用面试官智能体列表
     * <p>供 portal 开始面试时选择面试官人设；无可用 agent 时返回空列表（前端隐藏选择器）
     */
    @Operation(summary = "可用面试官列表", description = "列出启用状态的面试官智能体（id/名称/描述/开场白）")
    @GetMapping("/agents")
    public AjaxResult agents() {
        return AjaxResult.success(agentClient.listUsableAgents());
    }

    /**
     * 11. 提交自我介绍（v11.x 状态机 INTRO_WAITING 阶段）
     * <p>ScoringEngine 4 维度评分存 intro_score_json → 生成追问（INTRO_FOLLOWUP）或进入首题
     */
    @Operation(summary = "提交自我介绍", description = "4维度评分（逻辑结构/自我认知/岗位匹配/表达流畅），生成追问或进入首题")
    @PostMapping("/{id:[0-9]+}/self-intro")
    @RateLimiter(key = "voice:selfintro", time = 3600, count = 30)
    public AjaxResult selfIntro(@PathVariable("id") Long id, @RequestBody SelfIntroRequest body) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        if (body == null || body.getTranscript() == null || body.getTranscript().isBlank()) {
            return AjaxResult.error("自我介绍内容不能为空");
        }
        return AjaxResult.success(voiceInterviewService.submitSelfIntro(id, userId, body.getTranscript()));
    }

    /**
     * 11a. 按题目 ID 生成分级提示（v11.30 补建：语音演示页 useInterviewHint 调用）
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
     * 11b. 按题目 ID 提取关键词（v11.30 补建：语音演示页 useInterviewHint 调用）
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

    /**
     * 12. 启用中的岗位模板列表（v11.x 智能出题）
     * <p>供 portal 开始面试时选择岗位模板（job 题源 + 出题权重默认值）；未配置时返回空列表
     */
    @Operation(summary = "启用中的岗位模板列表", description = "返回 active 状态岗位模板（id/名称/类别/难度），供开始面试时选择")
    @GetMapping("/job-templates")
    public AjaxResult jobTemplates() {
        return AjaxResult.success(voiceInterviewService.listActiveJobTemplates());
    }

    /** 自我介绍请求体 */
    @lombok.Data
    public static class SelfIntroRequest {
        /** 自我介绍文本（ASR 转写或手动输入） */
        private String transcript;
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
