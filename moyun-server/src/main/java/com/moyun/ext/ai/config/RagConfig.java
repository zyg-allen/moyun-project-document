package com.moyun.ext.ai.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RAG检索配置类
 *
 * <p>系统级RAG（检索增强生成）配置，包括混合检索、查询扩展等参数</p>
 *
 * @author laomao
 */
@Data
@Component
@ConfigurationProperties(prefix = "moyun-ai.rag")
public class RagConfig {

    /** 最小召回数量，确保至少召回这么多候选 */
    private int minRecallCount = 15;

    /** 是否记录详细日志 */
    private boolean verboseLogging = true;

    /** 对话历史最大消息数 */
    @Value("${chat.max-messages:20}")
    private int maxMessages = 20;

    /** 是否启用混合检索（向量+BM25） */
    private boolean enableHybridSearch = true;

    /** 是否启用查询扩展 */
    private boolean enableQueryExpansion = true;

    /** BM25检索权重（0-1） */
    private double bm25Weight = 0.3;

    /** 向量检索权重（0-1） */
    private double vectorWeight = 0.7;

    // ========== 新增功能配置 ==========

    /** 是否启用查询改写（Query Rewriting） */
    private boolean enableQueryRewriting = true;

    /** 是否启用Self-RAG验证 */
    private boolean enableSelfRag = true;

    /** Self-RAG最低相关性阈值 */
    private double selfRagMinRelevance = 0.5;

    /** Self-RAG最大LLM验证数量（超过此数量使用关键词评估） */
    private int selfRagMaxVerifyCount = 5;

    /** 是否启用对话摘要 */
    private boolean enableConversationSummary = true;

    /** 对话摘要触发阈值（超过此消息数触发摘要） */
    private int summaryThreshold = 20;

    /** 摘要后保留的最近消息数 */
    private int summaryKeepRecent = 10;

    /** 是否启用意图识别 */
    private boolean enableIntentRecognition = true;
}
