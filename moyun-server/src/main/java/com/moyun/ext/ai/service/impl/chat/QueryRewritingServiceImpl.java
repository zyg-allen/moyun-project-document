package com.moyun.ext.ai.service.impl.chat;

import com.moyun.ext.ai.entity.Agent;
import com.moyun.ext.ai.entity.ModelConfig;
import com.moyun.ext.ai.service.ModelConfigService;
import com.moyun.ext.ai.service.chat.QueryRewritingService;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询改写服务实现
 *
 * <p>使用LLM进行查询优化，提升RAG检索效果</p>
 *
 * @author laomao
 * @since 2025-12-12
 */
@Slf4j
@Service
public class QueryRewritingServiceImpl implements QueryRewritingService {

    @Autowired
    private ModelConfigService modelConfigService;

    /** 短查询阈值（字符数），低于此值不改写 */
    private static final int SHORT_QUERY_THRESHOLD = 5;

    /** 长查询阈值（字符数），超过此值需要分解 */
    private static final int LONG_QUERY_THRESHOLD = 50;

    /**
     * 改写用户查询
     */
    @Override
    public String rewriteQuery(String originalQuery, Agent agent) {
        if (!shouldRewrite(originalQuery)) {
            log.debug("📝 查询太短，跳过改写: {}", originalQuery);
            return originalQuery;
        }

        try {
            ChatLanguageModel chatModel = getChatModel();
            if (chatModel == null) {
                log.warn("⚠️ 无法获取LLM模型，跳过查询改写");
                return originalQuery;
            }

            String domain = agent.getDescription() != null ? agent.getDescription() : "通用领域";

            String systemPrompt = """
                你是一个专业的查询优化助手。你的任务是将用户的查询改写为更适合向量检索的形式。
                
                改写规则：
                1. 保持原始查询的核心意图不变
                2. 使用更专业、更具体的表述
                3. 添加相关的同义词或专业术语
                4. 去除口语化表达和语气词
                5. 如果查询已经很清晰专业，可以原样返回
                
                领域背景：%s
                
                只输出改写后的查询，不要添加任何解释。
                """.formatted(domain);

            String userPrompt = "请改写以下查询：\n" + originalQuery;

            var response = chatModel.chat(
                List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt))
            );

            String rewritten = response.aiMessage().text().trim();
            
            // 简单验证改写结果
            if (rewritten.isEmpty() || rewritten.length() > originalQuery.length() * 3) {
                log.warn("⚠️ 改写结果异常，使用原始查询");
                return originalQuery;
            }

            log.info("✅ 查询改写: \"{}\" → \"{}\"", originalQuery, rewritten);
            return rewritten;

        } catch (Exception e) {
            log.error("❌ 查询改写失败: {}", e.getMessage());
            return originalQuery;
        }
    }

    /**
     * 将查询分解为多个子查询
     */
    @Override
    public List<String> decomposeQuery(String originalQuery, Agent agent) {
        if (originalQuery.length() < LONG_QUERY_THRESHOLD) {
            return List.of(originalQuery);
        }

        try {
            ChatLanguageModel chatModel = getChatModel();
            if (chatModel == null) {
                return List.of(originalQuery);
            }

            String systemPrompt = """
                你是一个查询分析专家。请将复杂的用户问题分解为2-4个简单的子问题。
                
                分解规则：
                1. 每个子问题应该独立可回答
                2. 子问题应覆盖原问题的所有方面
                3. 避免重复的子问题
                4. 如果问题已经足够简单，返回原问题即可
                
                输出格式：每行一个子问题，不要编号。
                """;

            String userPrompt = "请分解以下问题：\n" + originalQuery;

            var response = chatModel.chat(
                List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt))
            );

            String result = response.aiMessage().text().trim();
            List<String> subQueries = new ArrayList<>();
            
            for (String line : result.split("\n")) {
                String trimmed = line.trim();
                // 移除可能的编号前缀
                trimmed = trimmed.replaceFirst("^[0-9]+[.、)）]\\s*", "");
                if (!trimmed.isEmpty() && trimmed.length() > 3) {
                    subQueries.add(trimmed);
                }
            }

            if (subQueries.isEmpty()) {
                return List.of(originalQuery);
            }

            log.info("✅ 查询分解: \"{}\" → {} 个子查询", originalQuery, subQueries.size());
            return subQueries;

        } catch (Exception e) {
            log.error("❌ 查询分解失败: {}", e.getMessage());
            return List.of(originalQuery);
        }
    }

    /**
     * 生成假设文档（HyDE）
     */
    @Override
    public String generateHypotheticalDocument(String originalQuery, Agent agent) {
        try {
            ChatLanguageModel chatModel = getChatModel();
            if (chatModel == null) {
                return null;
            }

            String domain = agent.getDescription() != null ? agent.getDescription() : "通用领域";

            String systemPrompt = """
                你是一个%s领域的专家。请根据用户的问题，写出一段可能包含答案的文档片段。
                
                要求：
                1. 用专业、正式的语言
                2. 内容应该像是从技术文档中摘录的
                3. 包含具体的技术细节和专业术语
                4. 长度控制在100-200字
                
                只输出文档片段，不要添加任何解释或前缀。
                """.formatted(domain);

            String userPrompt = "问题：" + originalQuery;

            var response = chatModel.chat(
                List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt))
            );

            String hypothetical = response.aiMessage().text().trim();
            log.info("✅ 生成假设文档 (HyDE): {} 字符", hypothetical.length());
            
            return hypothetical;

        } catch (Exception e) {
            log.error("❌ 生成假设文档失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 判断是否需要查询改写
     */
    @Override
    public boolean shouldRewrite(String query) {
        if (query == null || query.trim().length() < SHORT_QUERY_THRESHOLD) {
            return false;
        }

        // 如果查询包含明确的技术术语或专有名词，可能不需要改写
        // 简单启发式：如果是单个词且长度较短，不改写
        String trimmed = query.trim();
        if (!trimmed.contains(" ") && trimmed.length() < 10) {
            return false;
        }

        return true;
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
