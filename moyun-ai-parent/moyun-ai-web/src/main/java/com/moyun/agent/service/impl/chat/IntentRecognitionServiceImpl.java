package com.moyun.agent.service.impl.chat;

import com.moyun.agent.entity.Agent;
import com.moyun.agent.entity.ModelConfig;
import com.moyun.agent.service.ModelConfigService;
import com.moyun.agent.service.chat.IntentRecognitionService;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 意图识别服务实现
 *
 * <p>使用规则+LLM混合方式识别用户意图</p>
 *
 * @author laomao
 * @since 2025-12-12
 */
@Slf4j
@Service
public class IntentRecognitionServiceImpl implements IntentRecognitionService {

    @Autowired
    private ModelConfigService modelConfigService;

    /** 问候语关键词 */
    private static final String[] GREETING_KEYWORDS = {
        "你好", "您好", "hi", "hello", "嗨", "早上好", "下午好", "晚上好",
        "你是谁", "你能做什么", "介绍一下你自己"
    };

    /** 结束语关键词 */
    private static final String[] END_KEYWORDS = {
        "再见", "拜拜", "谢谢", "thanks", "bye", "结束", "退出"
    };

    /** 工具调用关键词 */
    private static final String[] TOOL_KEYWORDS = {
        "查询", "搜索", "计算", "天气", "时间", "日期", "翻译"
    };

    /**
     * 识别用户意图
     */
    @Override
    public IntentResult recognize(String userMessage, Agent agent) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return IntentResult.unknown(userMessage);
        }

        String message = userMessage.trim().toLowerCase();

        // 1. 规则匹配：问候语
        if (matchesAny(message, GREETING_KEYWORDS)) {
            log.info("🎯 意图识别: GREETING (规则匹配)");
            return IntentResult.greeting(userMessage, 0.95);
        }

        // 2. 规则匹配：结束语
        if (matchesAny(message, END_KEYWORDS)) {
            log.info("🎯 意图识别: END_CONVERSATION (规则匹配)");
            return new IntentResult(IntentType.END_CONVERSATION, 0.9, Map.of(), new String[0], userMessage);
        }

        // 3. 规则匹配：工具调用
        if (matchesAny(message, TOOL_KEYWORDS)) {
            Map<String, Object> slots = extractToolSlots(message);
            log.info("🎯 意图识别: TOOL_CALL (规则匹配), slots={}", slots);
            return new IntentResult(IntentType.TOOL_CALL, 0.8, slots, new String[0], userMessage);
        }

        // 4. 问号结尾通常是问答
        if (message.endsWith("?") || message.endsWith("？") || message.contains("怎么") 
            || message.contains("什么") || message.contains("如何") || message.contains("为什么")) {
            Map<String, Object> slots = Map.of("question", userMessage);
            log.info("🎯 意图识别: KNOWLEDGE_QA (问句特征)");
            return IntentResult.knowledgeQA(userMessage, 0.85, slots);
        }

        // 5. 使用LLM进行更复杂的意图识别
        try {
            return recognizeWithLLM(userMessage, agent);
        } catch (Exception e) {
            log.warn("⚠️ LLM意图识别失败，默认为知识问答: {}", e.getMessage());
            return IntentResult.knowledgeQA(userMessage, 0.5, Map.of("question", userMessage));
        }
    }

    /**
     * 带上下文的意图识别
     */
    @Override
    public IntentResult recognizeWithContext(String userMessage, String context, Agent agent) {
        // 简化实现：目前与无上下文版本相同
        // TODO: 后续可以将上下文传递给LLM进行更精准的意图识别
        return recognize(userMessage, agent);
    }

    /**
     * 判断是否需要更多信息
     */
    @Override
    public boolean needsMoreInfo(IntentResult result) {
        return result.getMissingSlots() != null && result.getMissingSlots().length > 0;
    }

    /**
     * 生成追问语句
     */
    @Override
    public String generateFollowUp(IntentResult result) {
        if (result.getMissingSlots() == null || result.getMissingSlots().length == 0) {
            return null;
        }

        StringBuilder followUp = new StringBuilder("请提供更多信息：");
        for (String slot : result.getMissingSlots()) {
            switch (slot) {
                case "location" -> followUp.append("请问您想查询哪个城市？");
                case "date" -> followUp.append("请问是哪一天？");
                case "time" -> followUp.append("请问具体时间是？");
                case "target" -> followUp.append("请问具体是什么？");
                default -> followUp.append("请提供").append(slot).append("信息。");
            }
        }

        return followUp.toString();
    }

    /**
     * 使用LLM进行意图识别
     */
    private IntentResult recognizeWithLLM(String userMessage, Agent agent) {
        ChatLanguageModel chatModel = getChatModel();
        if (chatModel == null) {
            return IntentResult.knowledgeQA(userMessage, 0.6, Map.of("question", userMessage));
        }

        String systemPrompt = """
            你是一个意图识别专家。请分析用户消息的意图，并返回以下格式：
            
            意图类型（只返回一个）：
            - GREETING: 问候、打招呼、自我介绍询问
            - KNOWLEDGE_QA: 知识问答、专业咨询
            - TASK_EXECUTION: 执行任务、操作请求
            - TOOL_CALL: 需要调用工具（天气、时间、计算等）
            - CLARIFICATION: 澄清、确认之前的内容
            - FEEDBACK: 反馈、评价
            - END_CONVERSATION: 结束对话
            - UNKNOWN: 无法判断
            
            返回格式（JSON）：
            {"intent": "意图类型", "confidence": 0.0-1.0, "reason": "判断理由"}
            
            只返回JSON，不要其他内容。
            """;

        String userPrompt = "用户消息：" + userMessage;

        try {
            var response = chatModel.chat(
                List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt))
            );

            String result = response.aiMessage().text().trim();
            
            // 简单解析JSON
            IntentType intent = parseIntentFromResponse(result);
            double confidence = parseConfidenceFromResponse(result);

            log.info("🎯 意图识别(LLM): {} (置信度: {})", intent, confidence);
            
            Map<String, Object> slots = new HashMap<>();
            slots.put("question", userMessage);
            
            return new IntentResult(intent, confidence, slots, new String[0], userMessage);

        } catch (Exception e) {
            log.error("❌ LLM意图识别失败: {}", e.getMessage());
            return IntentResult.knowledgeQA(userMessage, 0.5, Map.of("question", userMessage));
        }
    }

    /**
     * 从LLM响应中解析意图类型
     */
    private IntentType parseIntentFromResponse(String response) {
        for (IntentType type : IntentType.values()) {
            if (response.toUpperCase().contains(type.name())) {
                return type;
            }
        }
        return IntentType.KNOWLEDGE_QA; // 默认为知识问答
    }

    /**
     * 从LLM响应中解析置信度
     */
    private double parseConfidenceFromResponse(String response) {
        Pattern pattern = Pattern.compile("\"confidence\"\\s*:\\s*([0-9.]+)");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                return 0.7;
            }
        }
        return 0.7;
    }

    /**
     * 检查消息是否匹配关键词
     */
    private boolean matchesAny(String message, String[] keywords) {
        for (String keyword : keywords) {
            if (message.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 提取工具调用的槽位
     */
    private Map<String, Object> extractToolSlots(String message) {
        Map<String, Object> slots = new HashMap<>();
        
        // 简单的槽位提取逻辑
        if (message.contains("天气")) {
            slots.put("tool", "weather");
            // 尝试提取城市名
            Pattern cityPattern = Pattern.compile("(北京|上海|广州|深圳|杭州|成都|武汉|南京|西安|重庆|\\w+市?).*天气");
            Matcher matcher = cityPattern.matcher(message);
            if (matcher.find()) {
                slots.put("location", matcher.group(1));
            }
        }
        
        if (message.contains("时间") || message.contains("几点")) {
            slots.put("tool", "time");
        }
        
        if (message.contains("翻译")) {
            slots.put("tool", "translate");
        }
        
        return slots;
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
