package com.moyun.ext.ai.service.impl.chat;

import com.moyun.ext.ai.config.RagConfig;
import com.moyun.ext.ai.entity.ModelConfig;
import com.moyun.ext.ai.service.ModelConfigService;
import com.moyun.ext.ai.service.chat.SelfRagService;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.content.Content;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Self-RAG相关性验证服务实现
 *
 * <p>使用LLM对检索结果进行自我验证，确保高质量输出</p>
 *
 * @author laomao
 * @since 2025-12-12
 */
@Slf4j
@Service
public class SelfRagServiceImpl implements SelfRagService {

    @Autowired
    private ModelConfigService modelConfigService;

    @Autowired
    private RagConfig ragConfig;

    /** 默认相关性阈值 */
    private static final double DEFAULT_RELEVANCE_THRESHOLD = 0.6;

    /**
     * 验证单个内容与查询的相关性
     */
    @Override
    public double verifyRelevance(Content content, String query) {
        try {
            ChatLanguageModel chatModel = getChatModel();
            if (chatModel == null) {
                log.warn("⚠️ 无法获取LLM模型，使用默认相关性评分");
                return estimateRelevanceByKeyword(content.textSegment().text(), query);
            }

            String contentText = content.textSegment().text();
            // 限制内容长度避免token过多
            if (contentText.length() > 500) {
                contentText = contentText.substring(0, 500) + "...";
            }

            String systemPrompt = """
                你是一个相关性评估专家。请判断给定的文档片段与用户查询的相关程度。
                
                评分标准：
                - 1.0：完全相关，直接回答了问题
                - 0.8：高度相关，包含关键信息
                - 0.6：部分相关，有一些有用信息
                - 0.4：弱相关，只有少量相关内容
                - 0.2：几乎不相关
                - 0.0：完全不相关
                
                只输出一个0到1之间的数字，不要任何解释。
                """;

            String userPrompt = String.format("""
                用户查询：%s
                
                文档片段：%s
                
                相关性评分：""", query, contentText);

            var response = chatModel.chat(
                List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt))
            );

            String result = response.aiMessage().text().trim();
            
            // 提取数字
            Pattern pattern = Pattern.compile("([0-9]+(\\.[0-9]+)?)");
            Matcher matcher = pattern.matcher(result);
            if (matcher.find()) {
                double score = Double.parseDouble(matcher.group(1));
                score = Math.min(1.0, Math.max(0.0, score)); // 限制在0-1范围
                log.debug("📊 Self-RAG验证 - 相关性: {}", String.format("%.2f", score));
                return score;
            }

            // 解析失败，使用关键词估算
            return estimateRelevanceByKeyword(content.textSegment().text(), query);

        } catch (Exception e) {
            log.error("❌ Self-RAG验证失败: {}", e.getMessage());
            return estimateRelevanceByKeyword(content.textSegment().text(), query);
        }
    }

    /**
     * 批量验证并过滤不相关内容
     */
    @Override
    public List<Content> filterByRelevance(List<Content> contents, String query, double minRelevance) {
        if (contents == null || contents.isEmpty()) {
            return new ArrayList<>();
        }

        int maxVerifyCount = ragConfig.getSelfRagMaxVerifyCount();
        log.info("🔍 Self-RAG开始验证 {} 个检索结果，最低相关性: {}, 最大LLM验证数: {}", 
                contents.size(), minRelevance, maxVerifyCount);
        
        List<Content> filteredContents = new ArrayList<>();
        int verifyCount = 0;

        for (Content content : contents) {
            // 限制验证数量，避免过多LLM调用
            if (verifyCount >= maxVerifyCount) {
                // 超出限制的直接使用关键词评估
                double score = estimateRelevanceByKeyword(content.textSegment().text(), query);
                if (score >= minRelevance) {
                    filteredContents.add(content);
                }
                continue;
            }

            double relevance = verifyRelevance(content, query);
            verifyCount++;

            if (relevance >= minRelevance) {
                filteredContents.add(content);
                log.debug("✅ 保留内容 - 相关性: {}", String.format("%.2f", relevance));
            } else {
                log.debug("❌ 过滤内容 - 相关性: {} (低于阈值 {})", 
                    String.format("%.2f", relevance), minRelevance);
            }
        }

        log.info("✅ Self-RAG验证完成: {} → {} 个相关内容", contents.size(), filteredContents.size());
        return filteredContents;
    }

    /**
     * 判断检索结果是否足以回答问题
     */
    @Override
    public boolean canAnswerQuery(List<Content> contents, String query) {
        if (contents == null || contents.isEmpty()) {
            return false;
        }

        try {
            ChatLanguageModel chatModel = getChatModel();
            if (chatModel == null) {
                // 无法获取模型，简单判断：有内容就认为可以回答
                return !contents.isEmpty();
            }

            // 合并内容摘要
            StringBuilder contentSummary = new StringBuilder();
            for (int i = 0; i < Math.min(contents.size(), 3); i++) {
                String text = contents.get(i).textSegment().text();
                if (text.length() > 200) {
                    text = text.substring(0, 200) + "...";
                }
                contentSummary.append("片段").append(i + 1).append(": ").append(text).append("\n\n");
            }

            String systemPrompt = """
                你是一个信息完整性评估专家。请判断给定的文档片段是否包含足够的信息来回答用户的问题。
                
                只回答 YES 或 NO：
                - YES：文档包含足够信息可以回答问题
                - NO：文档信息不足，需要更多资料
                """;

            String userPrompt = String.format("""
                用户问题：%s
                
                可用文档：
                %s
                
                是否可以回答：""", query, contentSummary);

            var response = chatModel.chat(
                List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt))
            );

            String result = response.aiMessage().text().trim().toUpperCase();
            boolean canAnswer = result.contains("YES");
            
            log.info("📋 信息完整性检查: {} ({})", canAnswer ? "✅ 可回答" : "❌ 信息不足", result);
            return canAnswer;

        } catch (Exception e) {
            log.error("❌ 信息完整性检查失败: {}", e.getMessage());
            // 出错时保守处理，认为可以回答
            return !contents.isEmpty();
        }
    }

    /**
     * 基于关键词的简单相关性估算（不调用LLM）
     */
    private double estimateRelevanceByKeyword(String content, String query) {
        if (content == null || query == null) {
            return 0.0;
        }

        String contentLower = content.toLowerCase();
        String queryLower = query.toLowerCase();

        // 分词
        String[] queryWords = queryLower.split("[\\s,，。、？！]+");
        
        int matchCount = 0;
        int totalWords = 0;

        for (String word : queryWords) {
            if (word.length() < 2) continue; // 跳过单字
            totalWords++;
            if (contentLower.contains(word)) {
                matchCount++;
            }
        }

        if (totalWords == 0) {
            return contentLower.contains(queryLower) ? 0.8 : 0.3;
        }

        double coverage = (double) matchCount / totalWords;
        
        // 完全匹配原始查询额外加分
        if (contentLower.contains(queryLower)) {
            coverage = Math.min(1.0, coverage + 0.3);
        }

        return coverage;
    }

    /**
     * 获取聊天模型
     */
    private ChatLanguageModel getChatModel() {
        try {
            ModelConfig config = modelConfigService.getDefaultChatConfig();
            if (config == null) {
                return null;
            }
            return modelConfigService.createChatModel(config.getId());
        } catch (Exception e) {
            log.error("获取聊天模型失败: {}", e.getMessage());
            return null;
        }
    }
}
