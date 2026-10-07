package com.moyun.ext.aigateway.support;

import com.moyun.ext.ai.enums.AiSceneEnum;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 意图判断器（前置增强能力 · v14.72 P2-3 规则库扩充）
 *
 * <p>依据《AI能力统一接入层 — 完整方案文档》V2.0 §7.1。规则匹配优先（零成本），
 * 未命中时按场景特定规则推断，置信度过低由网关触发兜底追问。</p>
 *
 * <p><b>v14.72 P2-3 扩充要点（支撑 enable_intent_routing 安全开启）</b>：</p>
 * <ul>
 *   <li><b>规则库扩充</b>：通用对话控制（问候/继续/结束/感谢）+ 业务域路由
 *       （简历优化/解析、智能出题、财务分析）+ 求职学习咨询（攻略/职业建议/代码），
 *       正则预编译（旧版每轮 matches() 重复编译全量正则）；</li>
 *   <li><b>置信度分级</b>：每条规则独立置信度（强信号 0.9+，弱信号 0.75+），
 *       命中即达到或接近网关追问阈值，不再一律 1.0；</li>
 *   <li><b>兜底语义修正</b>：正常长度的自由文本默认维持当前场景（GENERAL 0.65，
 *       高于追问阈值 0.6）——旧版 UNKNOWN 恒为 0.3，任何未命中规则的结构化
 *       场景输入都会被误追问，这是意图路由默认关闭的根因；仅明确过短/无实义
 *       输入（&lt;4 字符或纯标点）才以低置信触发追问。</li>
 * </ul>
 *
 * @author laomao
 * @since 2026-09-09
 */
@Component
public class IntentClassifier {

    /** 追问阈值（网关按此判定是否兜底追问，低于该值触发） */
    static final double CLARIFICATION_THRESHOLD = 0.6;

    /** 兜底维持场景的置信度（必须 ≥ 追问阈值，否则自由文本被误追问） */
    private static final double GENERAL_CONFIDENCE = 0.65;

    /** 规则条目：预编译正则 → 意图 + 置信度 + 建议场景 */
    private record Rule(Pattern pattern, String intent, double confidence, String suggestedScene) {}

    /**
     * 规则库（有序：先具体后泛化，前命中即返回）。
     * suggestedScene 非空时网关校验场景存在后才切换，不存在则维持原场景。
     */
    private static final List<Rule> RULES = new ArrayList<>();

    static {
        // —— 通用对话控制（强信号，无需切场景）——
        add("(?i).*\\b(hi|hello)\\b.*", "GREETING", 0.95, null);
        add(".*(你好|您好|早上好|下午好|晚上好|哈喽).*", "GREETING", 0.95, null);
        add(".*(谢谢|感谢|多谢|辛苦了).*", "THANKS", 0.95, null);
        add(".*(继续|下一题|下一个|再来一题|再问一个|接着来).*", "NEXT", 0.9, null);
        add(".*(结束|完成|好了|到此结束|就到这里|面试结束).*", "FINISH", 0.9, null);

        // —— 业务域路由（命中后建议切到对应场景；列在泛化规则前防被抢先匹配）——
        add(".*(简历优化|优化简历|改简历|润色简历|简历.{0,6}(改|优化|润色)|简历.{0,4}(问题|建议|点评)).*",
                "RESUME_OPTIMIZE", 0.9, AiSceneEnum.RESUME_OPTIMIZE.getCode());
        add(".*(简历解析|解析.{0,4}简历|简历提取|识别简历).*",
                "RESUME_PARSE", 0.9, AiSceneEnum.RESUME_PARSE.getCode());
        add(".*(简历).{0,10}(怎么写|如何写|怎么写好|怎么准备).*",
                "RESUME_HELP", 0.85, null);
        add(".*(出题|出几道|来几道题|给我出.{0,12}题|生成.{0,8}题|面试题库|练习题).*",
                "QUESTION_GENERATE", 0.85, AiSceneEnum.QUESTION_GENERATE.getCode());
        add(".*(财务分析|收支|消费分析|开销|账单|记账分析|这个月花了多少钱).*",
                "FINANCE_ANALYSIS", 0.85, AiSceneEnum.FINANCE_ANALYSIS.getCode());
        add(".*(模拟面试|面试练习|开始面试|练习面试|来一场面试).*",
                "MOCK_INTERVIEW", 0.85, null);

        // —— 求职/学习咨询（留在当前场景，多为 default_chat 泛问答）——
        add(".*(面试技巧|自我介绍|职业规划|跳槽|选 offer|offer 选择|求职|薪资谈判).*",
                "CAREER_ADVICE", 0.8, null);
        add(".*(代码|编程|报错|异常|调试|debug|bug|编译失败).*",
                "CODE_QUESTION", 0.75, null);
        add(".*(怎么|如何|怎样|步骤|教程|方法|支招).*", "HOW_TO", 0.75, null);
    }

    private static void add(String regex, String intent, double confidence, String suggestedScene) {
        RULES.add(new Rule(Pattern.compile(regex, Pattern.DOTALL), intent, confidence, suggestedScene));
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
            return unknown("输入为空，无法判断意图");
        }

        // 1. 通用规则匹配（快速，零成本，前命中即返回）
        for (Rule rule : RULES) {
            if (rule.pattern().matcher(userInput).matches()) {
                IntentResult result = new IntentResult(rule.intent(), rule.confidence());
                result.setSuggestedScene(rule.suggestedScene());
                return result;
            }
        }

        // 2. 场景特定意图
        if (AiSceneEnum.VOICE_INTERVIEW.getCode().equals(currentScene)) {
            if (userInput.contains("不知道") || userInput.contains("不清楚") || userInput.length() < 10) {
                return new IntentResult("CONFUSED", 0.85);
            }
            if (userInput.length() > 100) {
                return new IntentResult("COMPLETE_ANSWER", 0.80);
            }
        }
        if (AiSceneEnum.DEFAULT_CHAT.getCode().equals(currentScene)
                && userInput.matches("(是的|对的|好的|不是|不用|没有).*")) {
            return new IntentResult("CONFIRM", 0.7);
        }

        // 3. 兜底：仅明确过短/无实义输入低置信触发追问；
        //    正常长度自由文本视为维持当前场景的通用咨询（不低于追问阈值，
        //    避免开启意图路由的结构化场景被误追问）
        String trimmed = userInput.trim();
        if (trimmed.length() < 4 || trimmed.matches("\\P{L}+")) {
            return unknown("输入过短或无实义内容，建议补充说明");
        }
        return new IntentResult("GENERAL", GENERAL_CONFIDENCE);
    }

    private static IntentResult unknown(String clarificationQuestion) {
        IntentResult result = new IntentResult("UNKNOWN", 0.3);
        result.setClarificationQuestion(clarificationQuestion);
        return result;
    }
}
