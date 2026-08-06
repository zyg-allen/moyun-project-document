package com.moyun.ext.ai.service.impl.chat;

import com.moyun.ext.ai.entity.ModelConfig;
import com.moyun.ext.ai.service.ModelConfigService;
import com.moyun.ext.ai.service.chat.ConversationSummaryService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 对话摘要服务实现
 *
 * <p>使用LLM将长对话历史压缩为摘要</p>
 *
 * @author laomao
 * @since 2025-12-12
 */
@Slf4j
@Service
public class ConversationSummaryServiceImpl implements ConversationSummaryService {

    @Autowired
    private ModelConfigService modelConfigService;

    /** 默认Token估算系数（中文约2字符/token） */
    private static final double TOKEN_RATIO = 0.5;

    /** 最大摘要长度（字符） */
    private static final int MAX_SUMMARY_LENGTH = 500;

    /**
     * 判断是否需要生成摘要
     */
    @Override
    public boolean needsSummary(List<ChatMessage> messages, int maxMessages) {
        if (messages == null || messages.isEmpty()) {
            return false;
        }
        return messages.size() > maxMessages;
    }

    /**
     * 生成对话摘要
     */
    @Override
    public String generateSummary(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }

        try {
            ChatLanguageModel chatModel = getChatModel();
            if (chatModel == null) {
                log.warn("⚠️ 无法获取LLM模型，使用简单摘要");
                return generateSimpleSummary(messages);
            }

            // 构建对话文本
            StringBuilder conversationText = new StringBuilder();
            for (ChatMessage msg : messages) {
                if (msg instanceof UserMessage) {
                    conversationText.append("用户: ").append(((UserMessage) msg).singleText()).append("\n");
                } else if (msg instanceof AiMessage) {
                    String text = ((AiMessage) msg).text();
                    // 限制单条消息长度
                    if (text.length() > 200) {
                        text = text.substring(0, 200) + "...";
                    }
                    conversationText.append("助手: ").append(text).append("\n");
                }
            }

            String systemPrompt = """
                你是一个对话摘要专家。请将以下对话历史压缩为一段简洁的摘要。
                
                摘要要求：
                1. 保留对话的主要话题和关键信息
                2. 记录用户的主要需求和问题
                3. 保留重要的结论和答案
                4. 控制在200字以内
                5. 使用第三人称描述
                
                只输出摘要内容，不要添加任何前缀或解释。
                """;

            String userPrompt = "请为以下对话生成摘要：\n\n" + conversationText;

            var response = chatModel.chat(
                List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt))
            );

            String summary = response.aiMessage().text().trim();
            
            // 限制摘要长度
            if (summary.length() > MAX_SUMMARY_LENGTH) {
                summary = summary.substring(0, MAX_SUMMARY_LENGTH) + "...";
            }

            log.info("✅ 生成对话摘要: {} 条消息 → {} 字符摘要", messages.size(), summary.length());
            return summary;

        } catch (Exception e) {
            log.error("❌ 生成对话摘要失败: {}", e.getMessage());
            return generateSimpleSummary(messages);
        }
    }

    /**
     * 压缩对话历史
     */
    @Override
    public List<ChatMessage> compressHistory(List<ChatMessage> messages, int keepRecent) {
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }

        if (messages.size() <= keepRecent) {
            return new ArrayList<>(messages);
        }

        List<ChatMessage> result = new ArrayList<>();

        // 1. 将旧消息生成摘要
        int summaryEndIndex = messages.size() - keepRecent;
        List<ChatMessage> oldMessages = messages.subList(0, summaryEndIndex);
        String summary = generateSummary(oldMessages);

        // 2. 添加摘要作为系统消息
        if (!summary.isEmpty()) {
            result.add(new SystemMessage("【历史对话摘要】\n" + summary));
        }

        // 3. 添加最近的消息
        result.addAll(messages.subList(summaryEndIndex, messages.size()));

        log.info("📦 对话历史压缩: {} 条 → {} 条 (摘要+最近{}条)", 
            messages.size(), result.size(), keepRecent);

        return result;
    }

    /**
     * 更新增量摘要
     */
    @Override
    public String updateSummary(String existingSummary, List<ChatMessage> newMessages) {
        if (newMessages == null || newMessages.isEmpty()) {
            return existingSummary;
        }

        try {
            ChatLanguageModel chatModel = getChatModel();
            if (chatModel == null) {
                // 简单拼接
                String newPart = generateSimpleSummary(newMessages);
                return existingSummary + " " + newPart;
            }

            StringBuilder newConversation = new StringBuilder();
            for (ChatMessage msg : newMessages) {
                if (msg instanceof UserMessage) {
                    newConversation.append("用户: ").append(((UserMessage) msg).singleText()).append("\n");
                } else if (msg instanceof AiMessage) {
                    String text = ((AiMessage) msg).text();
                    if (text.length() > 150) {
                        text = text.substring(0, 150) + "...";
                    }
                    newConversation.append("助手: ").append(text).append("\n");
                }
            }

            String systemPrompt = """
                你是一个对话摘要专家。请更新已有的对话摘要，整合新的对话内容。
                
                要求：
                1. 保留原摘要中的重要信息
                2. 整合新对话的关键内容
                3. 去除重复信息
                4. 保持摘要简洁，控制在250字以内
                
                只输出更新后的摘要，不要添加任何前缀。
                """;

            String userPrompt = String.format("""
                已有摘要：
                %s
                
                新增对话：
                %s
                
                更新后的摘要：""", existingSummary, newConversation);

            var response = chatModel.chat(
                List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt))
            );

            String updated = response.aiMessage().text().trim();
            log.info("✅ 更新对话摘要: {} → {} 字符", existingSummary.length(), updated.length());
            return updated;

        } catch (Exception e) {
            log.error("❌ 更新摘要失败: {}", e.getMessage());
            return existingSummary + " [新增对话...]";
        }
    }

    /**
     * 估算Token数
     */
    @Override
    public int estimateTokenCount(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }

        int totalChars = 0;
        for (ChatMessage msg : messages) {
            if (msg instanceof UserMessage) {
                totalChars += ((UserMessage) msg).singleText().length();
            } else if (msg instanceof AiMessage) {
                totalChars += ((AiMessage) msg).text().length();
            } else if (msg instanceof SystemMessage) {
                totalChars += ((SystemMessage) msg).text().length();
            }
        }

        return (int) (totalChars * TOKEN_RATIO);
    }

    /**
     * 生成简单摘要（不使用LLM）
     */
    private String generateSimpleSummary(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }

        StringBuilder summary = new StringBuilder("对话主题包括：");
        int count = 0;

        for (ChatMessage msg : messages) {
            if (msg instanceof UserMessage && count < 3) {
                String text = ((UserMessage) msg).singleText();
                if (text.length() > 50) {
                    text = text.substring(0, 50) + "...";
                }
                summary.append(text).append("；");
                count++;
            }
        }

        return summary.toString();
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
