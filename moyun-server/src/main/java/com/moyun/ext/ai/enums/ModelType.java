package com.moyun.ext.ai.enums;

/**
 * 模型类型枚举
 *
 * <p>对应数据库表 model_config.model_type 字段，标识模型的业务用途：
 * <ul>
 *   <li>{@link #CHAT}        - 对话模型（如 qwen-plus, gpt-4）</li>
 *   <li>{@link #EMBEDDING}    - 向量化模型（如 text-embedding-v3）</li>
 *   <li>{@link #MULTIMODAL}   - 多模态模型（视觉理解，如 qwen-vl-plus）</li>
 *   <li>{@link #RERANKER}     - 重排序模型（如 qwen3-rerank）</li>
 *   <li>{@link #ASR}          - 语音识别模型（V10.0 语音面试官，如 paraformer-v2）</li>
 *   <li>{@link #TTS}          - 语音合成模型（V10.0 语音面试官，如 cosyvoice-v1）</li>
 * </ul>
 *
 * <p>注意：多模态模型在 DB 中通常存为 model_type='chat'，通过模型名（含 -vl/vision）识别；
 * 本枚举的 MULTIMODAL 主要用于业务层分类展示，不一定直接落库。
 *
 * <p>V10.0 起 ASR/TTS 用于语音面试官：浏览器端默认走 Web Speech API（免费、离线），
 * 服务端 ASR/TTS 模型仅在浏览器不支持或需高精度场景降级使用。
 *
 * @author moyun
 */
public enum ModelType {

    CHAT("chat", "对话模型"),
    EMBEDDING("embedding", "向量化模型"),
    MULTIMODAL("multimodal", "多模态模型"),
    RERANKER("reranker", "重排序模型"),
    ASR("asr", "语音识别模型"),
    TTS("tts", "语音合成模型");

    private final String code;
    private final String desc;

    ModelType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ModelType fromCode(String code) {
        if (code == null) {
            return CHAT;
        }
        for (ModelType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return CHAT;
    }
}
