package com.moyun.ext.cms.service.interview;

import com.moyun.portal.domain.entity.PortalInterviewQuestion;
import com.moyun.portal.domain.entity.PortalVoiceInterviewQA;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 面试提示词组装器（Agent 模式）
 *
 * <p>职责：
 * <ul>
 *   <li>{@link #renderSystemPrompt}：渲染 agent.systemPrompt 的 {{占位符}}（缺失渲染"无"）</li>
 *   <li>{@link #buildHistory}：qa 链 → 多轮 ChatMessage 历史（assistant=提问，user=回答）</li>
 *   <li>{@link #buildTaskDirective}：拼装任务协议（当前问题+转写+决策参考+题库候选+进度）</li>
 * </ul></p>
 *
 * <p>人设/协议分离：agent.systemPrompt 只管"你是谁、怎么表现"；
 * 输出协议与评分 schema 全部由本类代码拼装，后台改人设不会破坏协议。</p>
 *
 * @author moyun
 */
@Component
public class InterviewPromptAssembler {

    /** 占位符语法：{{key}} */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)}}");

    /** 历史消息单条内容截断长度 */
    private static final int HISTORY_ITEM_MAX_LEN = 600;

    /** 注入 LLM 的题库候选数量 */
    private static final int CANDIDATE_LIMIT = 8;

    /** 渲染 agent.systemPrompt 占位符；未提供的 key 渲染为"无" */
    public String renderSystemPrompt(String systemPrompt, Map<String, String> placeholders) {
        if (systemPrompt == null || systemPrompt.isBlank()) {
            return "";
        }
        Matcher m = PLACEHOLDER.matcher(systemPrompt);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String key = m.group(1);
            String value = placeholders.get(key);
            String replacement = (value == null || value.isBlank()) ? "无" : Matcher.quoteReplacement(value);
            m.appendReplacement(sb, replacement);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * 从 qa 链构建多轮对话历史（assistant=面试官提问话术，user=候选人回答）
     *
     * @param qaList   按 id 升序的 QA 列表
     * @param maxTurns 最多保留的问答对数量（agent.maxHistoryTurns）
     */
    public List<ChatMessage> buildHistory(List<PortalVoiceInterviewQA> qaList, int maxTurns) {
        List<ChatMessage> messages = new ArrayList<>();
        if (qaList == null || qaList.isEmpty()) {
            return messages;
        }
        // 收集已作答的问答对（userAnswer 非空才算一轮完整交互）
        List<PortalVoiceInterviewQA> answered = new ArrayList<>();
        for (PortalVoiceInterviewQA qa : qaList) {
            if (qa.getUserAnswer() != null && !qa.getUserAnswer().isBlank()) {
                answered.add(qa);
            }
        }
        int from = Math.max(0, answered.size() - Math.max(1, maxTurns));
        for (int i = from; i < answered.size(); i++) {
            PortalVoiceInterviewQA qa = answered.get(i);
            String question = qa.getQuestion() == null ? "" : qa.getQuestion();
            // 提问用 question（题目本身），话术中的冗余开场由 reply 承担
            messages.add(AiMessage.from(truncate(question)));
            messages.add(new UserMessage("（我的回答）" + truncate(qa.getUserAnswer())));
        }
        return messages;
    }

    /**
     * 拼装本轮任务指令（作为最后一条 UserMessage 的尾段）
     */
    public String buildTaskDirective(TaskContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n【当前问题】").append(ctx.getQuestionTitle() == null ? "" : ctx.getQuestionTitle()).append("\n");
        if (ctx.getQuestionAnalysis() != null && !ctx.getQuestionAnalysis().isBlank()) {
            sb.append("该题考察要点：").append(ctx.getQuestionAnalysis()).append("\n");
        }
        sb.append("【候选人的回答】（语音转写，可能口语化有噪音）：\n「")
          .append(truncate(ctx.getTranscript() == null ? "" : ctx.getTranscript(), 2000))
          .append("」\n\n【本轮任务】\n")
          .append("先直接输出你作为面试官的口头回应（60字以内，口语化，无任何前缀），\n")
          .append("然后另起一行输出 ```json 代码块，内容严格遵循如下结构（不要输出任何其他文字）：\n")
          .append("{\n")
          .append("  \"score\": 0-100整数,\n")
          .append("  \"dimensions\": {\"relevance\":0-100,\"professionalism\":0-100,\"fluency\":0-100,\"logic\":0-100,\"confidence\":0-100},\n")
          .append("  \"feedback\": \"两三句中文点评，先肯定再指出问题\",\n")
          .append("  \"flaws\": [\"回答暴露的具体漏洞，每条一句，最多3条\"],\n")
          .append("  \"redFlags\": [\"可疑信号：答非所问/背诵痕迹/前后矛盾/夸大数据，最多2条，没有则空数组\"],\n")
          .append("  \"sentiment\": {\"state\":\"nervous|confident|hesitant|calm\",\"note\":\"一句话依据\"},\n")
          .append("  \"fluencyAssessment\": {\"score\":0-100,\"comment\":\"口头禅/重复/停顿与连贯性一句话评价\"},\n")
          .append("  \"completeness\": {\"covered\":[\"已覆盖要点\"],\"missing\":[\"缺失的关键要点\"]},\n")
          .append("  \"level\": \"junior|mid|senior\",\n")
          .append("  \"followupWorth\": true或false,\n")
          .append("  \"nextAction\": \"deepen|change_topic|wrap_up\",\n")
          .append("  \"nextQuestion\": \"deepen或change_topic时的下一个问题（deepen必须引用候选人原话中的具体表述）；wrap_up时为收尾话术\",\n")
          .append("  \"candidateId\": 采用题库候选出题时填候选编号，自拟则为null,\n")
          .append("  \"transition\": \"换题或收尾时的过渡话术（20-40字）\",\n")
          .append("  \"guidance\": \"回答明显跑偏时一句引导语，否则空字符串\"\n")
          .append("}\n\n")
          .append("【nextAction 判定参考】\n")
          .append("- deepen：回答存在明显漏洞、模糊表述或值得深挖的细节（nextQuestion 引用原话追问）\n")
          .append("- change_topic：当前话题已考察充分，或候选人明显无法继续\n")
          .append("- wrap_up：主问题目已全部考察完毕\n");

        // 题库候选（换题参考）
        if (ctx.getCandidates() != null && !ctx.getCandidates().isEmpty()) {
            sb.append("\n【题库候选】（仅参考，可直接采用/改写/忽略；采用则在 candidateId 填编号）：\n");
            int idx = 1;
            for (PortalInterviewQuestion q : ctx.getCandidates()) {
                if (q == null || idx > CANDIDATE_LIMIT) {
                    break;
                }
                sb.append(idx).append(". ").append(truncate(q.getTitle(), 120));
                if (q.getTags() != null && !q.getTags().isBlank()) {
                    sb.append("（标签：").append(truncate(q.getTags(), 60)).append("）");
                }
                sb.append("\n");
                idx++;
            }
        }

        // 进度
        sb.append("\n【进度】已完成主问 ").append(ctx.getRoundsDone()).append("/").append(ctx.getTotalPlanned())
          .append(" 轮，当前追问链深 ").append(ctx.getFollowupDepth()).append("/2，剩余追问预算 ")
          .append(Math.max(0, InterviewDecisionPolicy.FOLLOWUP_BUDGET - ctx.getFollowupUsed())).append("/4\n");

        // 联网核查上下文（Phase 4 预留：WebSearchService 启用时注入）
        if (ctx.getWebSearchContext() != null && !ctx.getWebSearchContext().isBlank()) {
            sb.append("\n【联网核查结果】（对候选人声称的事实进行核实）：\n")
              .append(truncate(ctx.getWebSearchContext(), 1200)).append("\n");
        }

        sb.append("打分参考：完全跑题<30；浅层正确无细节50-65；框架正确有部分细节65-80；深入准确有权衡80+。");
        return sb.toString();
    }

    private String truncate(String s) {
        return truncate(s, HISTORY_ITEM_MAX_LEN);
    }

    private String truncate(String s, int maxLen) {
        if (s == null) {
            return "";
        }
        return s.length() > maxLen ? s.substring(0, maxLen) + "…" : s;
    }

    /** 单轮任务上下文 */
    public static class TaskContext {
        private String questionTitle;
        private String questionAnalysis;
        private String transcript;
        private int roundsDone;
        private int totalPlanned;
        private int followupDepth;
        private int followupUsed;
        private List<PortalInterviewQuestion> candidates;
        private String webSearchContext;

        public String getQuestionTitle() {
            return questionTitle;
        }

        public TaskContext setQuestionTitle(String questionTitle) {
            this.questionTitle = questionTitle;
            return this;
        }

        public String getQuestionAnalysis() {
            return questionAnalysis;
        }

        public TaskContext setQuestionAnalysis(String questionAnalysis) {
            this.questionAnalysis = questionAnalysis;
            return this;
        }

        public String getTranscript() {
            return transcript;
        }

        public TaskContext setTranscript(String transcript) {
            this.transcript = transcript;
            return this;
        }

        public int getRoundsDone() {
            return roundsDone;
        }

        public TaskContext setRoundsDone(int roundsDone) {
            this.roundsDone = roundsDone;
            return this;
        }

        public int getTotalPlanned() {
            return totalPlanned;
        }

        public TaskContext setTotalPlanned(int totalPlanned) {
            this.totalPlanned = totalPlanned;
            return this;
        }

        public int getFollowupDepth() {
            return followupDepth;
        }

        public TaskContext setFollowupDepth(int followupDepth) {
            this.followupDepth = followupDepth;
            return this;
        }

        public int getFollowupUsed() {
            return followupUsed;
        }

        public TaskContext setFollowupUsed(int followupUsed) {
            this.followupUsed = followupUsed;
            return this;
        }

        public List<PortalInterviewQuestion> getCandidates() {
            return candidates;
        }

        public TaskContext setCandidates(List<PortalInterviewQuestion> candidates) {
            this.candidates = candidates;
            return this;
        }

        public String getWebSearchContext() {
            return webSearchContext;
        }

        public TaskContext setWebSearchContext(String webSearchContext) {
            this.webSearchContext = webSearchContext;
            return this;
        }
    }
}
