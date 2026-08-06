package com.moyun.ext.ai.service.chat;

import com.moyun.ext.ai.entity.Agent;

import java.util.Map;

/**
 * 意图识别服务接口
 *
 * <p>识别用户消息的意图，支持多轮对话中的槽位填充：
 * <ul>
 *   <li>意图分类 - 识别用户想做什么</li>
 *   <li>槽位提取 - 提取关键参数</li>
 *   <li>上下文理解 - 结合对话历史理解意图</li>
 * </ul>
 * </p>
 *
 * @author laomao
 * @since 2025-12-12
 */
public interface IntentRecognitionService {

    /**
     * 识别用户意图
     *
     * @param userMessage 用户消息
     * @param agent       智能体配置
     * @return 意图识别结果
     */
    IntentResult recognize(String userMessage, Agent agent);

    /**
     * 带上下文的意图识别
     *
     * @param userMessage   用户消息
     * @param context       对话上下文
     * @param agent         智能体配置
     * @return 意图识别结果
     */
    IntentResult recognizeWithContext(String userMessage, String context, Agent agent);

    /**
     * 判断是否需要更多信息（槽位未填充完整）
     *
     * @param result 意图识别结果
     * @return true如果需要追问
     */
    boolean needsMoreInfo(IntentResult result);

    /**
     * 生成追问语句
     *
     * @param result 意图识别结果
     * @return 追问语句
     */
    String generateFollowUp(IntentResult result);

    /**
     * 意图类型枚举
     */
    enum IntentType {
        /** 问候/闲聊 */
        GREETING,
        /** 知识问答 */
        KNOWLEDGE_QA,
        /** 任务执行 */
        TASK_EXECUTION,
        /** 工具调用 */
        TOOL_CALL,
        /** 澄清/确认 */
        CLARIFICATION,
        /** 反馈/评价 */
        FEEDBACK,
        /** 结束对话 */
        END_CONVERSATION,
        /** 未知意图 */
        UNKNOWN
    }

    /**
     * 意图识别结果
     */
    class IntentResult {
        private final IntentType intent;
        private final double confidence;
        private final Map<String, Object> slots;
        private final String[] missingSlots;
        private final String originalMessage;

        public IntentResult(IntentType intent, double confidence, Map<String, Object> slots, 
                          String[] missingSlots, String originalMessage) {
            this.intent = intent;
            this.confidence = confidence;
            this.slots = slots;
            this.missingSlots = missingSlots;
            this.originalMessage = originalMessage;
        }

        public IntentType getIntent() { return intent; }
        public double getConfidence() { return confidence; }
        public Map<String, Object> getSlots() { return slots; }
        public String[] getMissingSlots() { return missingSlots; }
        public String getOriginalMessage() { return originalMessage; }
        
        public boolean hasSlot(String name) {
            return slots != null && slots.containsKey(name);
        }
        
        public Object getSlot(String name) {
            return slots != null ? slots.get(name) : null;
        }

        public static IntentResult unknown(String message) {
            return new IntentResult(IntentType.UNKNOWN, 0.0, Map.of(), new String[0], message);
        }

        public static IntentResult greeting(String message, double confidence) {
            return new IntentResult(IntentType.GREETING, confidence, Map.of(), new String[0], message);
        }

        public static IntentResult knowledgeQA(String message, double confidence, Map<String, Object> slots) {
            return new IntentResult(IntentType.KNOWLEDGE_QA, confidence, slots, new String[0], message);
        }
    }
}
