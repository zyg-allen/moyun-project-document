package com.moyun.ext.ai2.controller;

import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.ai2.constant.AiErrorCodes;
import com.moyun.ext.ai2.model.AiExecuteRequest;
import com.moyun.ext.ai2.model.AiExecuteResponse;
import com.moyun.ext.ai2.service.AiGatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 内容安全检测 Controller（v11.57 P0-3 场景收口：sensitive_word 业务入口走网关）
 *
 * <p>给运营/审核人员提供 LLM 级文本复核工具——DFA 词树只能字面匹配，
 * 本接口经统一网关（{@code sensitive_word} 场景）识别变体/语义级风险，
 * 自动享受网关横切能力（限流/成本熔断/执行日志/Prompt 注入防护）。</p>
 *
 * <p>路径前缀 /cms/ai/safety（admin 鉴权体系），业务内部直调
 * {@link AiGatewayService#execute}，不经开放入口 /api/ai/execute（open_api 白名单）。</p>
 *
 * @author laomao
 * @since 2026-09-11
 */
@Slf4j
@Tag(name = "AI内容安全检测", description = "LLM 级文本敏感内容复核工具（走统一网关）")
@RestController
@RequestMapping("/cms/ai/safety")
public class AiSafetyController extends BaseController {

    /** 与网关 PromptInjectionGuard.MAX_INPUT_LENGTH 保持一致 */
    private static final int MAX_TEXT_LENGTH = 8000;

    @Autowired
    private AiGatewayService aiGatewayService;

    @Operation(summary = "文本敏感内容检测", description = "检测文本是否包含敏感内容（涉政/色情/暴恐/辱骂/违法广告等），返回命中词/风险分级/处理建议")
    @PreAuthorize("@ss.hasPermi('cms:ai:safety:detect')")
    @Log(title = "AI内容安全检测", businessType = BusinessType.OTHER)
    @PostMapping("/detect")
    public AjaxResult detect(@RequestBody Map<String, String> body) {
        String text = body == null ? null : body.get("text");
        if (text == null || text.isBlank()) {
            return error("待检测文本不能为空");
        }
        if (text.length() > MAX_TEXT_LENGTH) {
            return error("待检测文本不能超过 " + MAX_TEXT_LENGTH + " 字符");
        }

        AiExecuteRequest request = new AiExecuteRequest();
        request.setSceneCode("sensitive_word");
        // 待检测文本走数据通道（input.text）：是数据不是指令，不走意图分类；
        // Handler 内部以 wrapData 分隔符隔离，防内容本身构成注入
        Map<String, Object> input = new HashMap<>();
        input.put("text", text);
        request.setInput(input);
        request.setUserId(getUserId());

        AiExecuteResponse<?> resp = aiGatewayService.execute(request);
        if (resp.getCode() == null || resp.getCode() != AiErrorCodes.SUCCESS) {
            return error(resp.getMsg() != null ? resp.getMsg() : "检测失败，请稍后重试");
        }
        // 返回检测结果 + 可观测元数据（requestId/耗时/模型，便于在执行日志页追溯）
        Map<String, Object> result = new HashMap<>();
        result.put("data", resp.getData());
        result.put("requestId", resp.getRequestId());
        result.put("elapsedMs", resp.getElapsedMs());
        if (resp.getMetadata() != null) {
            result.put("modelUsed", resp.getMetadata().getModelUsed());
            result.put("tokenUsed", resp.getMetadata().getTokenUsed());
        }
        return success(result);
    }
}
