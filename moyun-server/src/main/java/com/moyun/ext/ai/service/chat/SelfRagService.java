package com.moyun.ext.ai.service.chat;

import dev.langchain4j.rag.content.Content;

import java.util.List;

/**
 * Self-RAG相关性验证服务接口
 *
 * <p>对RAG检索结果进行自我验证，确保返回内容与查询高度相关：
 * <ul>
 *   <li>相关性评估 - 判断检索内容是否与查询相关</li>
 *   <li>信息完整性验证 - 检查是否包含足够信息回答问题</li>
 *   <li>自动重检索 - 相关性低时触发重新检索</li>
 * </ul>
 * </p>
 *
 * @author laomao
 * @since 2025-12-12
 */
public interface SelfRagService {

    /**
     * 验证单个内容与查询的相关性
     *
     * @param content 检索到的内容
     * @param query   用户查询
     * @return 相关性分数 (0-1)
     */
    double verifyRelevance(Content content, String query);

    /**
     * 批量验证并过滤不相关内容
     *
     * @param contents 检索到的内容列表
     * @param query    用户查询
     * @param minRelevance 最低相关性阈值
     * @return 过滤后的相关内容列表
     */
    List<Content> filterByRelevance(List<Content> contents, String query, double minRelevance);

    /**
     * 判断检索结果是否足以回答问题
     *
     * @param contents 检索到的内容列表
     * @param query    用户查询
     * @return true如果可以回答，false如果需要更多信息
     */
    boolean canAnswerQuery(List<Content> contents, String query);

    /**
     * 验证结果封装类
     */
    class VerificationResult {
        private final boolean relevant;
        private final double score;
        private final String reason;

        public VerificationResult(boolean relevant, double score, String reason) {
            this.relevant = relevant;
            this.score = score;
            this.reason = reason;
        }

        public boolean isRelevant() { return relevant; }
        public double getScore() { return score; }
        public String getReason() { return reason; }
    }
}
