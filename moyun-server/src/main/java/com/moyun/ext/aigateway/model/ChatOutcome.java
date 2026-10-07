package com.moyun.ext.aigateway.model;

import lombok.Data;
import lombok.Getter;

/**
 * LLM 调用结构化结果（可观测性闭环）
 *
 * <p>chatDetailed() 的返回值：除回复文本外，携带实际使用的模型与 token 消耗，
 * 供 Handler 填充 {@link AiMetadata}——网关响应从此可观测"用了哪个模型/花了多少token"。</p>
 *
 * @author laomao
 * @since 2026-09-10
 */
@Data
public class ChatOutcome {

    /** 回复文本（null/空 = 调用失败） */
    @Getter
    private String text;

    /** 实际使用的模型名（绑定模型调用成功时为模型名；默认模型回落时为 "default"；未知为 null） */
    private String modelUsed;

    /** 模型提供商（来自 ChatResponseMetadata.modelName() 的提供商段；未知为 null） */
    private String modelProvider;

    /** Token 消耗（输入+输出合计；模型未返回为 null） */
    private Integer tokenUsed;

    /** 输入 Token（成本核算：模型未返回为 null） */
    private Integer inputTokens;

    /** 输出 Token（成本核算：模型未返回为 null） */
    private Integer outputTokens;

    /**
     * 是否降级执行（v14.72）：true = 绑定模型瞬时异常重试后仍失败/返回空内容，
     * 回落默认模型完成（或经 FallbackStrategy 兜底）。业务侧可据此区分
     * "正常结果"与"降级结果"，避免把兜底文案当模型产出二次消费。
     */
    private boolean degraded;

    /** 瞬时异常重试次数（0=一次成功；1=重试一次后成功） */
    private Integer retryCount;

    public boolean isSuccess() {
        return text != null && !text.isBlank();
    }
}
