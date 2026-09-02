package com.moyun.portal.controller;

import com.moyun.common.constant.HttpStatus;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.cms.domain.vo.AiTaskVO;
import com.moyun.ext.cms.domain.vo.ResumeDeepOptimizeVO;
import com.moyun.ext.cms.domain.vo.ResumeOptimizeTaskVO;
import com.moyun.ext.cms.domain.vo.UserResumeVO;
import com.moyun.ext.cms.service.AiTaskService;
import com.moyun.ext.cms.service.DeepOptimizeTaskHandler;
import com.moyun.ext.cms.service.IUserResumeService;
import com.moyun.ext.cms.service.ResumeDeepOptimizeService;
import com.moyun.ext.cms.service.ResumeJobMatchService;
import com.moyun.portal.domain.entity.PortalResumeJobMatch;
import com.moyun.portal.domain.entity.PortalResumeJobTarget;
import com.moyun.portal.domain.entity.PortalResumeScoreReport;
import com.moyun.portal.mapper.PortalResumeJobTargetMapper;
import com.moyun.portal.mapper.PortalResumeScoreReportMapper;
import com.moyun.portal.util.PortalSecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 简历优化工作台 Controller（门户端，v10.13 简历优化重构）
 * <p>
 * 链路：选岗位(填JD) → 选简历 → AI岗位匹配评分(存报告) → 深度优化(前后对比逐项采纳)
 *       → 预览微调 → 保存新版本 → 重新评分。
 * <p>
 * 接口列表：
 *   GET/POST/PUT/DELETE  /portal/resume/optimize/job-target    岗位目标 CRUD
 *   POST  /portal/resume/optimize/match/{resumeId}/{jobTargetId}  执行匹配分析
 *   GET   /portal/resume/optimize/match/{resumeId}/latest        最近匹配报告
 *   POST  /portal/resume/optimize/deep/{resumeId}/{jobTargetId}   生成深度优化建议（同步，兼容旧版）
 *   POST  /portal/resume/optimize/deep/{resumeId}/{jobTargetId}/async  提交深度优化异步任务（v10.19 推荐）
 *   GET   /portal/resume/optimize/deep/task/{taskId}              查询深度优化任务状态（前端轮询）
 *   POST  /portal/resume/optimize/deep/apply                      采纳建议并保存新版本
 *   GET   /portal/resume/optimize/history/{resumeId}              优化历史
 *
 * @author moyun
 */
@Tag(name = "简历优化工作台", description = "岗位匹配评分与深度优化闭环")
@RestController
@RequestMapping("/portal/resume/optimize")
public class PortalResumeOptimizeController extends BaseController {

    @Autowired
    private PortalResumeJobTargetMapper jobTargetMapper;

    @Autowired
    private ResumeJobMatchService jobMatchService;

    @Autowired
    private ResumeDeepOptimizeService deepOptimizeService;

    @Autowired
    private IUserResumeService userResumeService;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    /** 评分报告存档 Mapper（v10.18 阶段五） */
    @Autowired
    private PortalResumeScoreReportMapper scoreReportMapper;

    /** v10.23：通用 AI 异步任务服务（深度优化异步任务切换到 portal_ai_task） */
    @Autowired
    private AiTaskService aiTaskService;

    private Long currentUserId() {
        return PortalSecurityUtils.getUserId();
    }

    // ==================== 岗位目标 CRUD ====================

    @Operation(summary = "我的岗位目标列表")
    @GetMapping("/job-target")
    public AjaxResult listJobTargets() {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        List<PortalResumeJobTarget> list = jobTargetMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PortalResumeJobTarget>()
                        .eq(PortalResumeJobTarget::getUserId, userId)
                        .orderByDesc(PortalResumeJobTarget::getIsDefault)
                        .orderByDesc(PortalResumeJobTarget::getId));
        return AjaxResult.success(list);
    }

    @Operation(summary = "新建岗位目标", description = "岗位名称与JD为必填")
    @PostMapping("/job-target")
    public AjaxResult createJobTarget(@RequestBody PortalResumeJobTarget dto) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        if (dto.getPosition() == null || dto.getPosition().trim().isEmpty()) {
            return AjaxResult.error("请填写目标岗位名称");
        }
        if (dto.getJdText() == null || dto.getJdText().trim().isEmpty()) {
            return AjaxResult.error("请粘贴岗位JD描述（匹配分析的核心输入）");
        }
        // 设为默认时清除原默认
        if (dto.getIsDefault() != null && dto.getIsDefault() == 1) {
            clearDefault(userId);
        }
        dto.setId(null);
        dto.setUserId(userId);
        jobTargetMapper.insert(dto);
        return AjaxResult.success(dto.getId());
    }

    @Operation(summary = "更新岗位目标")
    @PutMapping("/job-target/{id}")
    public AjaxResult updateJobTarget(@PathVariable Long id, @RequestBody PortalResumeJobTarget dto) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        PortalResumeJobTarget exist = jobTargetMapper.selectById(id);
        if (exist == null || !exist.getUserId().equals(userId)) {
            return AjaxResult.error("岗位目标不存在或无权访问");
        }
        if (dto.getIsDefault() != null && dto.getIsDefault() == 1) {
            clearDefault(userId);
        }
        dto.setId(id);
        dto.setUserId(userId);
        jobTargetMapper.updateById(dto);
        return AjaxResult.success();
    }

    @Operation(summary = "删除岗位目标")
    @DeleteMapping("/job-target/{id}")
    public AjaxResult deleteJobTarget(@PathVariable Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        PortalResumeJobTarget exist = jobTargetMapper.selectById(id);
        if (exist == null || !exist.getUserId().equals(userId)) {
            return AjaxResult.error("岗位目标不存在或无权访问");
        }
        jobTargetMapper.deleteById(id);
        return AjaxResult.success();
    }

    private void clearDefault(Long userId) {
        PortalResumeJobTarget upd = new PortalResumeJobTarget();
        upd.setIsDefault(0);
        jobTargetMapper.update(upd,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<PortalResumeJobTarget>()
                        .eq(PortalResumeJobTarget::getUserId, userId)
                        .eq(PortalResumeJobTarget::getIsDefault, 1));
    }

    // ==================== 岗位匹配分析 ====================

    @Operation(summary = "执行岗位匹配分析", description = "LLM 四维分析（关键词/经验/技能/结构）+ 规则兜底，结果存档可追溯")
    @PostMapping("/match/{resumeId}/{jobTargetId}")
    public AjaxResult match(@PathVariable Long resumeId, @PathVariable Long jobTargetId) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        UserResumeVO resume = userResumeService.selectResumeDetail(resumeId, userId);
        if (resume == null) {
            return AjaxResult.error("简历不存在或无权访问");
        }
        try {
            PortalResumeJobMatch report = jobMatchService.analyze(userId, resume, jobTargetId);
            return AjaxResult.success(report);
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Operation(summary = "最近匹配报告", description = "查询简历最近一次匹配分析结果（无则 data 为 null）")
    @GetMapping("/match/{resumeId}/latest")
    public AjaxResult latestMatch(@PathVariable Long resumeId) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(jobMatchService.latestReport(userId, resumeId));
    }

    // ==================== 深度优化 ====================

    @Operation(summary = "生成深度优化建议（同步，兼容旧版）", description = "LLM 基于JD逐项生成优化建议（需 AI 模型）。注意：长耗时场景建议改用 /deep/{resumeId}/{jobTargetId}/async 异步接口")
    @PostMapping("/deep/{resumeId}/{jobTargetId}")
    public AjaxResult deepOptimize(@PathVariable Long resumeId, @PathVariable Long jobTargetId) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        UserResumeVO resume = userResumeService.selectResumeDetail(resumeId, userId);
        if (resume == null) {
            return AjaxResult.error("简历不存在或无权访问");
        }
        try {
            ResumeDeepOptimizeVO vo = deepOptimizeService.generate(resume, jobTargetId);
            return AjaxResult.success(vo);
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Operation(summary = "提交深度优化异步任务（v10.19 推荐；v10.23 切换通用 AI 任务表）",
            description = "立即返回任务ID，后端异步调用 LLM 生成建议。前端通过 GET /deep/task/{taskId} 轮询任务状态，"
                    + "status=success 时 result 字段为优化结果（ResumeDeepOptimizeVO）。"
                    + "解决大模型调用超时问题，支持关闭页面后回来查看。")
    @PostMapping("/deep/{resumeId}/{jobTargetId}/async")
    public AjaxResult deepOptimizeAsync(@PathVariable Long resumeId, @PathVariable Long jobTargetId) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        try {
            // 保留原有提交前校验（简历归属/岗位目标存在/AI 可用性）
            deepOptimizeService.validateDeepOptimizeSubmit(userId, resumeId, jobTargetId);
            // 委托通用 AI 任务基础设施（v10.23：portal_ai_task 统一承载）
            Map<String, Object> bizRef = new HashMap<>();
            bizRef.put("resumeId", resumeId);
            bizRef.put("jobTargetId", jobTargetId);
            Long taskId = aiTaskService.submitTask(userId, DeepOptimizeTaskHandler.TASK_TYPE, bizRef);
            return AjaxResult.success(taskId);
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Operation(summary = "查询深度优化任务状态",
            description = "前端轮询调用：返回 status(pending/running/success/failed)、progress(0-100)、result(成功时为优化结果)、errorMsg(失败时)。"
                    + "v10.23：改读通用 AI 任务表 portal_ai_task，返回结构映射保持旧版字段兼容。")
    @GetMapping("/deep/task/{taskId}")
    public AjaxResult deepOptimizeTaskStatus(@PathVariable Long taskId) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        try {
            AiTaskVO task = aiTaskService.getTask(taskId, userId);
            if (!DeepOptimizeTaskHandler.TASK_TYPE.equals(task.getTaskType())) {
                return AjaxResult.error("任务不存在或无权访问");
            }
            // 映射为旧 ResumeOptimizeTaskVO 结构，保证前端轮询字段不破坏
            ResumeOptimizeTaskVO vo = new ResumeOptimizeTaskVO();
            vo.setTaskId(task.getId());
            vo.setStatus(task.getStatus());
            vo.setResult(task.getResult());
            vo.setErrorMsg(task.getError());
            // 粗粒度进度估算
            switch (task.getStatus() == null ? "" : task.getStatus()) {
                case "pending":  vo.setProgress(10); break;
                case "running":   vo.setProgress(50); break;
                case "success":   vo.setProgress(100); break;
                //case "failed":    vo.setProgress(0); break;
                default:          vo.setProgress(0);
            }
            return AjaxResult.success(vo);
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Operation(summary = "采纳建议并保存优化结果",
            description = "参数：resumeId、jobTargetId、optimize(完整建议列表)、adopted(采纳的下标数组)；直接更新原简历（幂等）并记录优化历史")
    @PostMapping("/deep/apply")
    public AjaxResult applyOptimize(@RequestBody Map<String, Object> params) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        try {
            Long resumeId = Long.valueOf(String.valueOf(params.get("resumeId")));
            UserResumeVO resume = userResumeService.selectResumeDetail(resumeId, userId);
            if (resume == null) {
                return AjaxResult.error("简历不存在或无权访问");
            }
            ResumeDeepOptimizeVO optimize = objectMapper.convertValue(params.get("optimize"), ResumeDeepOptimizeVO.class);
            @SuppressWarnings("unchecked")
            List<Integer> adopted = (List<Integer>) params.get("adopted");
            Long newId = deepOptimizeService.applyAndSave(userId, resume, optimize, adopted,
                    (vo, uid) -> userResumeService.saveResume(vo, uid));
            return AjaxResult.success(newId);
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            return AjaxResult.error("参数解析失败：" + e.getMessage());
        }
    }

    @Operation(summary = "优化历史列表")
    @GetMapping("/history/{resumeId}")
    public AjaxResult history(@PathVariable Long resumeId) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(deepOptimizeService.listHistory(userId, resumeId));
    }

    // ==================== AI 实时辅助编辑（v10.14 设计文档 P0 需求#2） ====================

    @Operation(summary = "字段级 AI 实时辅助",
            description = "编辑页字段旁「✨AI优化」：对工作/项目描述、自我评价、技能清单生成3个差异化优化版本，用户采纳替换")
    @PostMapping("/ai-assist")
    public AjaxResult aiAssist(@RequestBody Map<String, Object> params) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        try {
            String field = String.valueOf(params.get("field"));
            String originalText = params.get("originalText") == null ? "" : String.valueOf(params.get("originalText"));
            String position = params.get("position") == null ? null : String.valueOf(params.get("position"));
            @SuppressWarnings("unchecked")
            List<String> skillNames = (List<String>) params.get("skillNames");
            return AjaxResult.success(deepOptimizeService.fieldAssist(field, originalText, position, skillNames));
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Operation(summary = "AI 填充空字段（v10.22）",
            description = "为空的工作经历/项目经历/自我介绍生成初始草稿，基于已有信息（姓名/技能/求职意向/教育经历）+ 可选目标岗位JD。"
                    + "与 ai-assist 区别：ai-assist 是对已有内容生成3个优化版本，本接口为空字段生成初始内容供用户采纳填充到表单。"
                    + "返回结构：{works:[WorkItem...], projects:[ProjectItem...], selfIntro:String, message:String}")
    @PostMapping("/ai-draft/{resumeId:[0-9]+}")
    public AjaxResult aiDraftEmptyFields(
            @PathVariable("resumeId") Long resumeId,
            @RequestParam(value = "jobTargetId", required = false) Long jobTargetId) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        try {
            return AjaxResult.success(deepOptimizeService.aiDraftEmptyFields(resumeId, userId, jobTargetId));
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    // ==================== 评分报告存档（v10.18 设计文档 P1 需求#4） ====================

    @Operation(summary = "保存评分报告",
            description = "触发评分（同步写 portal_user_resume 评分字段）并存档为可追溯报告；"
                    + "可选参数 source（manual/optimize/template，默认 manual）、jobTargetId、position 用于标记评分来源与关联岗位")
    @PostMapping("/score-report")
    public AjaxResult saveScoreReport(@RequestBody Map<String, Object> params) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        try {
            Long resumeId = Long.valueOf(String.valueOf(params.get("resumeId")));
            UserResumeVO resume = userResumeService.selectResumeDetail(resumeId, userId);
            if (resume == null) {
                return AjaxResult.error("简历不存在或无权访问");
            }
            // 触发评分（内部已自动入库 source=manual）
            userResumeService.scoreResume(resumeId, userId);

            // 取刚插入的报告（按 id 倒序第一条）
            PortalResumeScoreReport latest = scoreReportMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PortalResumeScoreReport>()
                            .eq(PortalResumeScoreReport::getUserId, userId)
                            .eq(PortalResumeScoreReport::getResumeId, resumeId)
                            .orderByDesc(PortalResumeScoreReport::getId)
                            .last("LIMIT 1"));
            if (latest == null) {
                return AjaxResult.error("评分失败：未生成报告");
            }

            // 若调用方指定了 source（非 manual）/jobTargetId/position，补全这条报告
            String source = params.get("source") == null ? null : String.valueOf(params.get("source")).trim();
            Object jobTargetIdObj = params.get("jobTargetId");
            String position = params.get("position") == null ? null : String.valueOf(params.get("position")).trim();

            boolean needUpdate = false;
            if (source != null && !source.isEmpty() && !"manual".equals(source)) {
                latest.setSource(source);
                needUpdate = true;
            }
            if (jobTargetIdObj != null && !"".equals(String.valueOf(jobTargetIdObj))) {
                try {
                    latest.setJobTargetId(Long.valueOf(String.valueOf(jobTargetIdObj)));
                    needUpdate = true;
                } catch (NumberFormatException ignore) {
                    // 非数字 jobTargetId 忽略
                }
            }
            if (position != null && !position.isEmpty()) {
                latest.setPositionSnapshot(position);
                needUpdate = true;
            }
            if (needUpdate) {
                scoreReportMapper.updateById(latest);
            }
            return AjaxResult.success(latest);
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            return AjaxResult.error("参数解析失败：" + e.getMessage());
        }
    }

    @Operation(summary = "评分报告列表", description = "查询简历的评分历史报告，按时间倒序（可追溯历史评分）")
    @GetMapping("/score-report/{resumeId}")
    public AjaxResult listScoreReports(@PathVariable Long resumeId) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        List<PortalResumeScoreReport> list = scoreReportMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PortalResumeScoreReport>()
                        .eq(PortalResumeScoreReport::getUserId, userId)
                        .eq(PortalResumeScoreReport::getResumeId, resumeId)
                        .orderByDesc(PortalResumeScoreReport::getCreateTime)
                        .orderByDesc(PortalResumeScoreReport::getId));
        return AjaxResult.success(list);
    }
}
