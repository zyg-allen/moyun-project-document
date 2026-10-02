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
 * 通用 AI 异步任务 Controller（门户端）
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

    @Autowired
    private com.moyun.vip.service.IVipService vipService;

    /**
     * **会员权益门禁登记表**：taskType → {platform, benefit, consume, 错误文案}。
     *
     * <p>为什么必须有：通用任务入口 {@code POST /portal/ai/task/submit} 接受任意 taskType，
     * 而各专用入口上的 {@code @VipOnly} 只作用于**那个**方法 —— 于是
     * {@code taskType=deep_optimize} 可经本入口提交，**完全绕过**深度优化的会员校验与次数扣减
     * （清单 P1：付费能力被白嫖）。</p>
     *
     * <p>取值必须与专用入口的注解一致（此处 deep_optimize 对齐
     * {@code PortalResumeOptimizeController#deepOptimizeAsync} 的
     * {@code @VipOnly(platform="portal", benefit="resume_optimize")}，含 consume=true 与 402 语义）。
     * 新增受权益保护的异步任务类型时，**在此登记**即可，无需改动别处。</p>
     */
    private static final Map<String, VipGate> VIP_GATED_TASKS = Map.of(
            "deep_optimize", new VipGate("portal", "resume_optimize", true, "简历深度优化次数已用完，请开通会员")
    );

    /** 受权益保护的异步任务类型（见 {@link #VIP_GATED_TASKS}） */
    private record VipGate(String platform, String benefit, boolean consume, String message) {
    }

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
            // ── 会员权益门禁（与专用入口同口径）──
            // 通用入口若不校验，deep_optimize 可绕过 @VipOnly 直接提交 ⇒ 付费能力被白嫖。
            VipGate gate = taskType == null ? null : VIP_GATED_TASKS.get(taskType);
            if (gate != null) {
                boolean pass = gate.consume()
                        ? vipService.consumeBenefit(userId, gate.platform(), gate.benefit())
                        : vipService.hasBenefit(userId, gate.platform(), gate.benefit());
                if (!pass) {
                    // 与 @VipOnly 一致：402 语义（前端据此弹开通引导）
                    return AjaxResult.error(402, gate.message());
                }
            }
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
