package com.moyun.ext.ai2.handler.impl;

import com.moyun.ext.ai.entity.AiSceneConfig;
import com.moyun.ext.ai2.constant.AiErrorCodes;
import com.moyun.ext.ai2.handler.AbstractAiSceneHandler;
import com.moyun.ext.ai2.model.AiExecuteRequest;
import com.moyun.ext.ai2.model.AiExecuteResponse;
import com.moyun.ext.ai2.model.data.InterviewSceneData;
import com.moyun.ext.ai2.support.PromptInjectionGuard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * 面试交互场景Handler（scene = voice_interview）
 *
 * <p>职责：基于岗位/简历上下文评估候选人回答（同步评估）或流式输出追问。
 * 模型选择复用底座场景绑定（ai_scene_config.voice_interview）。</p>
 *
 * <p><strong>v11.58 P0-3c 业务收口——双子任务契约（task+context）：</strong></p>
 * <ul>
 *   <li><b>task=answer_analysis</b>：候选人回答深度分析（评分校正/6维/漏洞/水平/追问建议），
 *       原 {@code VoiceInterviewServiceImpl.analyzeAnswerByLlm} 提示词逐字收编，
 *       输出置于 {@link InterviewSceneData#getStructured()}。</li>
 *   <li><b>task=candidate_ask</b>：候选人反问环节的面试官回答（原
 *       {@code answerCandidateQuestion} LLM 分支），输出 {"text": ...}。</li>
 *   <li><b>task=knowledge_desc</b>：知识点批量一句话简介（原 {@code tryLlmKnowledgeDesc}），
 *       输出 {"points": [...]}。</li>
 *   <li><b>task=speak_text</b>：面试官轮次话术（原 {@code generateSpeakText}），输出 {"text": ...}。</li>
 *   <li><b>task=self_intro</b>：自我介绍 4 维评分（原 {@code ScoringEngine.tryLlmSelfIntro}），
 *       输出置于 {@link InterviewSceneData#getStructured()}。</li>
 *   <li><b>task 缺省</b>（管理台场景调试/开放入口）：评估+出题一体。</li>
 * </ul>
 *
 * <p>输入参数：task(子任务)、context(业务组装的面试官人设/题目/指令文本——self_intro 为岗位，
 * 可空)、transcript(候选人语音转写等外部不可信数据——经 wrapData 数据通道隔离，不走顶层
 * userInput 以避免意图分类器误打断)。默认路径：position(岗位)、question(当前问题)、
 * answer(候选人回答)、round(轮次，可选)、resumeSummary(简历摘要，可选)</p>
 *
 * @author laomao
 * @since 2026-09-09
 */
@Slf4j
@Component("voiceInterviewHandler")
public class VoiceInterviewHandler extends AbstractAiSceneHandler {

    @Override
    public String getSceneCode() {
        return "voice_interview";
    }

    @Override
    public String getSupportedOutputMode() {
        return "both";
    }

    @Override
    public void validate(AiExecuteRequest request) {
        String task = getInputString(request, "task");
        if (task == null || task.isBlank()) {
            requireInputString(request, "question");
            return;
        }
        switch (task) {
            case "answer_analysis", "candidate_ask" -> {
                requireInputString(request, "context");
                requireInputString(request, "transcript");
            }
            case "knowledge_desc", "speak_text" -> requireInputString(request, "context");
            // self_intro：context 为岗位（可空），transcript 必填
            case "self_intro" -> requireInputString(request, "transcript");
            default -> throw new IllegalArgumentException("不支持的子任务: " + task);
        }
    }

    @Override
    public AiExecuteResponse<?> execute(AiExecuteRequest request, AiSceneConfig config) {
        String task = getInputString(request, "task");
        if (task != null && !task.isBlank()) {
            switch (task) {
                case "answer_analysis" -> {
                    return executeAnswerAnalysis(request);
                }
                case "candidate_ask" -> {
                    return executeCandidateAsk(request);
                }
                case "knowledge_desc" -> {
                    return executeKnowledgeDesc(request);
                }
                case "speak_text" -> {
                    return executeSpeakText(request);
                }
                case "self_intro" -> {
                    return executeSelfIntro(request);
                }
                default -> {
                    return AiExecuteResponse.failure(AiErrorCodes.INVALID_REQUEST,
                            "不支持的子任务: " + task);
                }
            }
        }
        return executeEvaluate(request, config);
    }

    // ==================== 子任务：候选人回答深度分析（v11.58 P0-3c 收口） ====================

    /**
     * 原 VoiceInterviewServiceImpl.analyzeAnswerByLlm 提示词逐字收编：
     * context = 面试官人设 + 题目 + 考察要点（业务侧组装）；transcript = 语音转写回答。
     */
    private AiExecuteResponse<?> executeAnswerAnalysis(AiExecuteRequest request) {
        String context = requireInputString(request, "context");
        String transcript = requireInputString(request, "transcript");

        String systemPrompt = context
                + "\n候选人的语音转写回答见用户消息（可能口语化、有转写噪音）。\n"
                + "请以严格的技术面试官标准分析该回答，只输出如下 JSON（不要任何其他文字）：\n"
                + "{\n"
                + "  \"score\": 0-100的整数,\n"
                + "  \"dimensions\": {\"relevance\": 0-100, \"professionalism\": 0-100, \"fluency\": 0-100, \"interactivity\": 0-100, \"confidence\": 0-100, \"logic\": 0-100},\n"
                + "维度定义：relevance=回答与问题的相关性；professionalism=技术深度与专业度；fluency=表达流畅度；interactivity=互动性（举例/对比/坦诚沟通）；confidence=自信笃定程度；logic=逻辑条理与结构。\n"
                + "  \"feedback\": \"两到三句中文点评，先肯定再指出问题\",\n"
                + "  \"flaws\": [\"回答中暴露的具体漏洞或模糊点，每条一句话，最多3条，没有则空数组\"],\n"
                + "  \"level\": \"junior或mid或senior，对候选人当前真实水平的判断\",\n"
                + "  \"followupWorth\": true或false，该回答是否存在值得追问的漏洞,\n"
                + "  \"followupQuestion\": \"若followupWorth为true，给出一句针对漏洞的追问；必须引用候选人回答中的具体表述\",\n"
                + "  \"guidance\": \"若回答明显跑偏，给出一句引导性提示，否则为空字符串\"\n"
                + "}\n"
                + "打分参考：完全跑题<30；浅层正确但无细节50-65；有正确框架和部分细节65-80；深入准确有取舍权衡80+。";

        String raw = chat(getSceneCode(), systemPrompt,
                PromptInjectionGuard.wrapData("候选人语音转写回答", transcript));
        if (raw == null || raw.isBlank()) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_CALL_FAILED, "AI服务暂不可用");
        }

        Map<String, Object> parsed = parseJsonMap(raw);
        if (parsed == null) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_PARSE_ERROR, "回答分析结果解析失败");
        }

        InterviewSceneData data = new InterviewSceneData();
        data.setStructured(parsed);
        return AiExecuteResponse.success(data);
    }

    // ==================== 子任务：候选人反问环节（v11.58 P0-3c 收口） ====================

    /** 原 answerCandidateQuestion LLM 分支提示词逐字收编 */
    private AiExecuteResponse<?> executeCandidateAsk(AiExecuteRequest request) {
        String context = requireInputString(request, "context");
        String transcript = requireInputString(request, "transcript");

        String systemPrompt = context
                + "\n现在进入候选人反问环节，请以面试官身份回答候选人的提问，回答要专业、简洁（150字以内）。";

        String raw = chat(getSceneCode(), systemPrompt,
                PromptInjectionGuard.wrapData("候选人提问", transcript));
        if (raw == null || raw.isBlank()) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_CALL_FAILED, "AI服务暂不可用");
        }

        String text = cleanLlmText(raw);
        if (text.isEmpty()) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_PARSE_ERROR, "面试官回答为空");
        }

        InterviewSceneData data = new InterviewSceneData();
        data.setStructured(Map.of("text", text));
        return AiExecuteResponse.success(data);
    }

    // ==================== 子任务：知识点批量简介（v11.58 P0-3c 收口） ====================

    /** 原 tryLlmKnowledgeDesc 提示词逐字收编：context = "面试岗位：X\n知识点：A、B、C" */
    private AiExecuteResponse<?> executeKnowledgeDesc(AiExecuteRequest request) {
        String context = requireInputString(request, "context");

        String systemPrompt = "你是面试知识点归纳助手，只输出 JSON。\n"
                + "为以下面试知识点各生成一句话简介（40字内，说明是什么+面试常考点，中文）。\n"
                + "输出 JSON：{\"points\":[{\"title\":\"知识点\",\"desc\":\"简介\"}]}，覆盖全部知识点，不要输出其他内容。";

        String raw = chat(getSceneCode(), systemPrompt,
                PromptInjectionGuard.wrapData("知识点归纳任务数据", context));
        if (raw == null || raw.isBlank()) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_CALL_FAILED, "AI服务暂不可用");
        }

        Map<String, Object> parsed = parseJsonMap(raw);
        if (parsed == null) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_PARSE_ERROR, "知识点简介解析失败");
        }

        InterviewSceneData data = new InterviewSceneData();
        data.setStructured(parsed);
        return AiExecuteResponse.success(data);
    }

    // ==================== 子任务：面试官轮次话术（v11.58 P0-3c 收口） ====================

    /** 原 generateSpeakText 提示词收编：context = 面试官人设；transcript = 轮次数据组装文本 */
    private AiExecuteResponse<?> executeSpeakText(AiExecuteRequest request) {
        String context = requireInputString(request, "context");
        String transcript = getInputString(request, "transcript");

        String userPrompt = transcript != null && !transcript.isBlank()
                ? PromptInjectionGuard.wrapData("面试轮次数据", transcript)
                : "请作为面试官给出简短回应（50字以内）。";

        String raw = chat(getSceneCode(), context, userPrompt);
        if (raw == null || raw.isBlank()) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_CALL_FAILED, "AI服务暂不可用");
        }

        String text = cleanLlmText(raw);
        if (text.isEmpty()) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_PARSE_ERROR, "面试官话术为空");
        }

        InterviewSceneData data = new InterviewSceneData();
        data.setStructured(Map.of("text", text));
        return AiExecuteResponse.success(data);
    }

    // ==================== 子任务：自我介绍 4 维评分（v11.58 P0-3c 收口） ====================

    /** 原 ScoringEngine.tryLlmSelfIntro 提示词逐字收编：context = 目标岗位（可空），transcript = 自我介绍转写 */
    private AiExecuteResponse<?> executeSelfIntro(AiExecuteRequest request) {
        String position = getInputString(request, "context");
        String transcript = requireInputString(request, "transcript");

        String systemPrompt = "你是一位资深技术面试官，请对候选人的自我介绍进行严格评估。"
                + (position != null && !position.isBlank() ? "目标岗位：" + position + "。" : "")
                + "只输出如下 JSON（不要任何其他文字）：\n"
                + "{\n"
                + "  \"scores\": {\"structure\": 0-100, \"awareness\": 0-100, \"matching\": 0-100, \"fluency\": 0-100},\n"
                + "  \"comment\": \"两到三句中文总评，先肯定亮点再指出不足\",\n"
                + "  \"strengths\": [\"1-2条亮点，每条一句话\"],\n"
                + "  \"weaknesses\": [\"1-2条不足，每条一句话\"],\n"
                + "  \"followupWorth\": true或false（自我介绍中是否有值得追问的模糊点）,\n"
                + "  \"followupQuestion\": \"followupWorth 为 true 时给出一句针对性追问，必须引用候选人原话\"\n"
                + "}\n"
                + "维度定义：structure=逻辑结构（条理/详略/结构词）；awareness=自我认知（优劣势/职业规划清晰度）；"
                + "matching=岗位匹配（技术栈/项目经历与目标岗位相关度）；fluency=表达流畅（口语自然度/信息密度）。\n"
                + "打分参考：结构混乱<40；基本连贯50-65；条理清晰有详略70-85；结构完整且亮点突出85+。";

        String raw = chat(getSceneCode(), systemPrompt,
                PromptInjectionGuard.wrapData("候选人自我介绍", transcript));
        if (raw == null || raw.isBlank()) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_CALL_FAILED, "AI服务暂不可用");
        }

        Map<String, Object> parsed = parseJsonMap(raw);
        if (parsed == null) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_PARSE_ERROR, "自我介绍评分解析失败");
        }

        InterviewSceneData data = new InterviewSceneData();
        data.setStructured(parsed);
        return AiExecuteResponse.success(data);
    }

    // ==================== 默认路径：评估 + 出题一体（管理台调试/开放入口） ====================

    private AiExecuteResponse<?> executeEvaluate(AiExecuteRequest request, AiSceneConfig config) {
        String position = getInputString(request, "position");
        String question = requireInputString(request, "question");
        String answer = getInputString(request, "answer");
        Integer round = getInputInteger(request, "round", 1);

        String systemPrompt = """
                你是资深技术面试官。基于岗位要求评估候选人回答，只输出 JSON：
                {"evaluation": "评估反馈(2-4句，指出亮点与不足)", "score": 0到100整数,
                 "question": "下一个追问或下一题", "questionType": "八股/算法/项目/场景",
                 "nextAction": "followup(需追问)/next(下一题)/end(面试结束)"}
                候选人回答过于简短或答非所问时 score 给低分并追问。禁止输出 JSON 以外内容。""";

        StringBuilder user = new StringBuilder();
        if (position != null) {
            user.append("岗位：").append(position).append("\n");
        }
        String resumeSummary = getInputString(request, "resumeSummary");
        if (resumeSummary != null) {
            user.append("候选人简历摘要：").append(resumeSummary).append("\n");
        }
        user.append("第").append(round).append("轮问题：").append(question).append("\n");
        // v11.58：候选人回答为外部不可信数据，切数据通道隔离（防提示词注入）
        if (answer != null && !answer.isBlank()) {
            user.append(PromptInjectionGuard.wrapData("候选人回答", answer));
        } else {
            user.append("候选人回答：（未作答）");
        }

        String raw = chat(getSceneCode(), systemPrompt, user.toString());
        if (raw == null || raw.isBlank()) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_CALL_FAILED, "AI服务暂不可用");
        }

        Map<String, Object> parsed = parseOutput(raw, config);
        if (parsed == null) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_PARSE_ERROR, "AI返回结果解析失败");
        }

        InterviewSceneData data = new InterviewSceneData();
        data.setQuestion(getStr(parsed, "question"));
        data.setQuestionType(getStr(parsed, "questionType"));
        data.setRound(round);
        data.setNextAction(getStr(parsed, "nextAction"));
        data.setEvaluation(getStr(parsed, "evaluation"));
        Object score = parsed.get("score");
        if (score instanceof Number number) {
            data.setScore(number.intValue());
        }
        return AiExecuteResponse.success(data);
    }

    @Override
    public void executeStream(AiExecuteRequest request, SseEmitter emitter) {
        String question = requireInputString(request, "question");
        String position = getInputString(request, "position");

        String systemPrompt = """
                你是资深技术面试官。针对候选人上一轮回答给出反馈，然后自然过渡到下一个问题。
                反馈要点 concise、专业、有建设性。""";

        StringBuilder user = new StringBuilder();
        if (position != null) {
            user.append("岗位：").append(position).append("\n");
        }
        user.append("上一轮问题：").append(question).append("\n");
        String answer = getInputString(request, "answer");
        if (answer != null && !answer.isBlank()) {
            user.append(PromptInjectionGuard.wrapData("候选人回答", answer));
        } else {
            user.append("候选人回答：（未作答）");
        }

        chatStream(getSceneCode(), systemPrompt, user.toString(), emitter, null);
    }

    private String getStr(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : null;
    }
}
