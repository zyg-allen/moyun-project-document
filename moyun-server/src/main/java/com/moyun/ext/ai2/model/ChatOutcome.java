package com.moyun.ext.ai2.model;

import lombok.Data;

/**
 * LLM 调用结构化结果（v11.51 可观测性闭环）
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
    private String text;

    /** 实际使用的模型名（绑定模型调用成功时为模型名；默认模型回落时为 "default"；未知为 null） */
    private String modelUsed;

    /** 模型提供商（来自 ChatResponseMetadata.modelName() 的提供商段；未知为 null） */
    private String modelProvider;

    /** Token 消耗（输入+输出合计；模型未返回为 null） */
    private Integer tokenUsed;

    public String getText() {
        return text;
    }

    public boolean isSuccess() {
        return text != null && !text.isBlank();
    }
}
