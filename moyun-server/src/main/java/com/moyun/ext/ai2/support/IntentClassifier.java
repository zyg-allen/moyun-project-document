package com.moyun.ext.ai2.support;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 意图判断器（前置增强能力）
 *
 * <p>依据《AI能力统一接入层 — 完整方案文档》V2.0 §7.1。规则匹配优先（零成本），
 * 未命中时按场景特定规则推断，置信度过低由网关触发兜底追问。</p>
 *
 * @author laomao
 * @since 2026-09-09
 */
@Component
public class IntentClassifier {

    /** 通用规则：正则 → 意图 */
    private static final Map<String, String> RULES = new LinkedHashMap<>();

    static {
        RULES.put(".*(退款|退货|取消).*", "REFUND");
        RULES.put(".*(怎么|如何|步骤|教程).*", "HOW_TO");
        RULES.put(".*(你好|早上好|下午好|晚上好).*", "GREETING");
        RULES.put(".*(继续|下一题|下一个).*", "NEXT");
        RULES.put(".*(结束|完成|好了).*", "FINISH");
    }

    /**
     * 意图判断结果
     */
    @Data
    public static class IntentResult {
        /** 意图标签 */
        private String intent;
        /** 置信度 [0,1] */
        private double confidence;
        /** 建议切换的场景（null 表示维持原场景） */
        private String suggestedScene;
        /** 置信度不足时的追问文案 */
        private String clarificationQuestion;

        public IntentResult(String intent, double confidence) {
            this.intent = intent;
            this.confidence = confidence;
        }
    }

    /**
     * 分类用户输入意图
     *
     * @param userInput     用户输入（可为 null）
     * @param currentScene  当前请求场景
     */
    public IntentResult classify(String userInput, String currentScene) {
        if (userInput == null || userInput.isBlank()) {
            return new IntentResult("UNKNOWN", 0.3);
        }

        // 1. 通用规则匹配（快速，零成本）
        for (Map.Entry<String, String> entry : RULES.entrySet()) {
            if (userInput.matches("(?s)" + entry.getKey())) {
                return new IntentResult(entry.getValue(), 1.0);
            }
        }

        // 2. 场景特定意图
        if ("voice_interview".equals(currentScene)) {
            if (userInput.contains("不知道") || userInput.contains("不清楚") || userInput.length() < 10) {
                return new IntentResult("CONFUSED", 0.85);
            }
            if (userInput.length() > 100) {
                return new IntentResult("COMPLETE_ANSWER", 0.80);
            }
        }

        // 3. 默认
        return new IntentResult("UNKNOWN", 0.3);
    }
}
