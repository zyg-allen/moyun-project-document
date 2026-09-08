package com.moyun.ext.ai2.handler.impl;

import com.moyun.ext.ai2.constant.AiErrorCodes;
import com.moyun.ext.ai2.handler.AbstractAiSceneHandler;
import com.moyun.ext.ai2.model.AiExecuteRequest;
import com.moyun.ext.ai2.model.AiExecuteResponse;
import com.moyun.ext.ai2.model.data.InterviewSceneData;
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
 * <p>输入参数：position(岗位)、question(当前问题)、answer(候选人回答)、round(轮次，可选)、
 * resumeSummary(简历摘要，可选)</p>
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
    public AiExecuteResponse<?> execute(AiExecuteRequest request) {
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
        user.append("候选人回答：").append(answer != null ? answer : "（未作答）");

        String raw = chat(getSceneCode(), systemPrompt, user.toString());
        if (raw == null || raw.isBlank()) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_CALL_FAILED, "AI服务暂不可用");
        }

        Map<String, Object> parsed = parseJsonMap(raw);
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
        user.append("候选人回答：").append(answer != null ? answer : "（未作答）");

        chatStream(getSceneCode(), systemPrompt, user.toString(), emitter, null);
    }

    private String getStr(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : null;
    }
}
