package com.moyun.ext.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 提供商注册表实体（V11.0.2 配置驱动改造）
 *
 * <p>提供商能力元数据落库，替代代码中硬编码的 switch(provider) 分支。
 * 模型工厂 / 流式判定 / 连接测试 / 多模态等链路统一按 {@code apiStyle} 分支，
 * 新增任何 OpenAI 兼容提供商（DeepSeek/Moonshot/智谱等）仅需在后台插入一条记录。</p>
 *
 * <p>关联：{@code ai_model_config.provider} → {@code ai_provider.code}（逻辑外键）</p>
 *
 * @author moyun
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("ai_provider")
public class AiProvider extends AiBaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 提供商编码（小写唯一，model_config.provider 关联值） */
    private String code;

    /** 显示名称 */
    private String name;

    /**
     * API 风格（真正的差异维度，工厂按此分支而非 provider 名）：
     * openai_compatible = OpenAI 兼容端点；ollama_native = Ollama 原生 API
     */
    public static final String STYLE_OPENAI_COMPATIBLE = "openai_compatible";
    public static final String STYLE_OLLAMA_NATIVE = "ollama_native";

    private String apiStyle;

    /** 默认 Base URL（模型配置留空时兜底） */
    private String defaultBaseUrl;

    /** 该提供商 chat 模型是否支持流式输出（streamingSupported 判定来源） */
    private Boolean supportsStreaming;

    /** 是否必须配置 API Key */
    private Boolean requiresApiKey;

    /** 是否启用（停用后不可选/不可用） */
    private Boolean enabled;

    /** 排序（小在前） */
    private Integer sortOrder;

    /** 备注 */
    private String remark;
}
