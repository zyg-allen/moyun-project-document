package com.moyun.portal.controller;

import com.moyun.common.constant.HttpStatus;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.cms.domain.vo.AiTaskVO;
import com.moyun.ext.cms.service.AiTaskService;
import com.moyun.portal.util.PortalSecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 通用 AI 异步任务 Controller（门户端，v10.23）
 *
 * <p>统一门户 LLM 长耗时任务（简历解析/岗位匹配/空字段草稿/深度优化）的提交与查询：
 * 提交立即返回任务 ID，后端线程池异步执行，前端轮询任务状态。</p>
 *
 * <p>接口列表：</p>
 * <ul>
 *   <li>POST /portal/ai/task/submit   提交 AI 异步任务（taskType + bizRef）</li>
 *   <li>GET  /portal/ai/task/{id}     查询任务状态（前端轮询）</li>
 * </ul>
 *
 * @author moyun
 */
@Tag(name = "通用AI异步任务", description = "统一异步任务提交与轮询（简历解析/岗位匹配/空字段草稿/深度优化）")
@RestController
@RequestMapping("/portal/ai/task")
public class PortalAiTaskController extends BaseController {

    @Autowired
    private AiTaskService aiTaskService;

    private Long currentUserId() {
        return PortalSecurityUtils.getUserId();
    }

    @Operation(summary = "提交 AI 异步任务",
            description = "taskType 取值：resume_parse（bizRef: resumeId/fileUrl/fileName）、job_match（bizRef: resumeId/jobTargetId）、"
                    + "ai_draft（bizRef: resumeId/jobTargetId 可空）、deep_optimize（bizRef: resumeId/jobTargetId）。"
                    + "立即返回 taskId，前端通过 GET /portal/ai/task/{id} 轮询任务状态。")
    @PostMapping("/submit")
    public AjaxResult submit(@RequestBody Map<String, Object> params) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期");
        }
        try {
            String taskType = params.get("taskType") == null ? null : String.valueOf(params.get("taskType")).trim();
            @SuppressWarnings("unchecked")
            Map<String, Object> bizRef = (Map<String, Object>) params.get("bizRef");
            Long taskId = aiTaskService.submitTask(userId, taskType, bizRef);
            Map<String, Object> data = new HashMap<>();
            data.put("taskId", taskId);
            return AjaxResult.success(data);
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Operation(summary = "查询 AI 任务状态",
            description = "前端轮询调用：返回 status(pending/running/success/failed)、progressMsg(进度提示)、"
                    + "result(成功时为任务结果，结构由 taskType 决定)、error(失败原因)。")
    @GetMapping("/{id:[0-9]+}")
    public AjaxResult getTask(@PathVariable("id") Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期");
        }
        try {
            AiTaskVO vo = aiTaskService.getTask(id, userId);
            return AjaxResult.success(vo);
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
    }
}
