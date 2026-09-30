package com.moyun.ext.cms.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.portal.domain.entity.PortalVoiceInterview;
import com.moyun.portal.domain.entity.PortalVoiceInterviewQA;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 面试会话「纯函数」支持类（v13.61 批次 4 四 / 第③步）。
 *
 * <p>承接拆分第①②步（{@link InterviewTextUtils} / {@link InterviewReportFormatter}），
 * 本类收口**会话编排中不依赖实例成员**的那部分逻辑：Redis 键、时长换算、配置读取、
 * 上下文指令拼装、VO 映射。全部 {@code static}，不注入依赖、不做 IO。</p>
 *
 * <p><b>为什么值得抽</b>：这些方法原先埋在 2700+ 行的 Service 里，
 * 与 SSE 生命周期、事务、锁混在一起；抽走后"编排"与"换算/拼装"边界清晰，
 * 且可被单测直接覆盖（无需构造 Spring 上下文）。</p>
 *
 * <p><b>不属于本类</b>：任何需要 Mapper / 锁 / 网关 / 线程池的方法
 * （如 {@code aggregateAndStoreReport}、{@code enhanceReportByAgent}、{@code runAgentTurn}）
 * —— 它们是真正的编排逻辑，留在 Service。</p>
 *
 * @author laomao
 */
public final class InterviewSessionSupport {

    private InterviewSessionSupport() {
    }

    /** 面试时长（分钟）合法区间与默认值（与 Service 内既有口径一致） */
    public static final int MIN_DURATION_MINUTES = 5;
    public static final int MAX_DURATION_MINUTES = 120;
    public static final int DEFAULT_DURATION_MINUTES = 20;
    /** 超时宽限（分钟）：超过「配置时长 + 宽限」才判定超时收口 */
    public static final int DURATION_GRACE_MINUTES = 2;

    /** 口头结束意图识别（严格短语，避免误判正常回答） */
    private static final Pattern VERBAL_END_PATTERN = Pattern.compile(
            "(结束面试|面试结束|就到这里|到这里吧|不面了|不想面了|停止面试|不用问了|可以了)");

    // ==================== Redis 键（统一前缀，避免散落魔法字符串） ====================

    /** 答题幂等锁键（同一 qaId 的并发/重复提交互斥） */
    public static String qaTurnLockKey(Long qaId) {
        return "voice:qa-turn:" + qaId;
    }

    /** 单场分析互斥锁键 */
    public static String analysisLockKey(Long interviewId) {
        return "voice:analysis:" + interviewId;
    }

    /** 全场提示计数器键（Redis INCR，跨实例原子） */
    public static String hintCounterKey(Long interviewId) {
        return "voice:hint-count:" + interviewId;
    }

    // ==================== 会话配置读取（纯解析） ====================

    /**
     * 从会话的 {@code configJson} 读一个字符串键（缺失/非法返回空串，不抛）。
     *
     * @param interview 会话（其 {@code configJson} 可为空）
     * @param key       配置键
     * @param mapper    调用方提供的 ObjectMapper
     * @return 值；不存在时为空串
     */
    public static String readConfigKey(PortalVoiceInterview interview, String key, ObjectMapper mapper) {
        if (interview == null || StringUtils.isEmpty(interview.getConfigJson())) {
            return "";
        }
        try {
            JsonNode node = mapper.readTree(interview.getConfigJson());
            if (node == null || !node.hasNonNull(key)) {
                return "";
            }
            return node.path(key).asText("");
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 解析时长（分钟）：会话内配置优先，其次全局配置，最后默认值；均钳制到合法区间。
     *
     * @param interview     会话
     * @param globalMinutes 全局配置值（{@code null} 表示未配置 → 用默认值）
     * @param mapper        调用方提供的 ObjectMapper
     * @return 合法时长（分钟）
     */
    public static int resolveDurationMinutes(PortalVoiceInterview interview, Integer globalMinutes,
                                             ObjectMapper mapper) {
        String v = readConfigKey(interview, "durationMinutes", mapper);
        if (!v.isEmpty()) {
            try {
                return clampDuration(Integer.parseInt(v.trim()));
            } catch (NumberFormatException ignored) {
                // 落到全局配置
            }
        }
        if (globalMinutes != null) {
            return clampDuration(globalMinutes);
        }
        return DEFAULT_DURATION_MINUTES;
    }

    /** 时长钳制到合法区间 */
    public static int clampDuration(int minutes) {
        return Math.max(MIN_DURATION_MINUTES, Math.min(MAX_DURATION_MINUTES, minutes));
    }

    /**
     * 剩余分钟数（可为负：已超时）。
     *
     * @param interview  会话（{@code createTime} 为空时返回整场时长）
     * @param durationMin 本场时长（分钟）
     * @return 剩余分钟
     */
    public static long remainMinutesOf(PortalVoiceInterview interview, int durationMin) {
        if (interview == null || interview.getCreateTime() == null) {
            return durationMin;
        }
        return Duration.between(LocalDateTime.now(),
                interview.getCreateTime().plusMinutes(durationMin)).toMinutes();
    }

    /**
     * 是否已超时（超过「时长 + 宽限」）。
     *
     * @param interview   会话（{@code createTime} 为空视为未超时）
     * @param durationMin 本场时长（分钟）
     * @return true 表示应拒绝继续作答并收口
     */
    public static boolean isInterviewTimedOut(PortalVoiceInterview interview, int durationMin) {
        if (interview == null || interview.getCreateTime() == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(
                interview.getCreateTime().plusMinutes(durationMin + DURATION_GRACE_MINUTES));
    }

    /** 候选人是否明确表达结束意图（严格短语匹配） */
    public static boolean matchesVerbalEnd(String transcript) {
        return transcript != null && VERBAL_END_PATTERN.matcher(transcript).find();
    }

    // ==================== 上下文指令拼装 ====================

    /**
     * 每轮任务指令：只约束话术形态，不参与出题决策。
     *
     * <p>v13.62：按已答轮数注入分阶段引导（开场深挖 → 专业考察 → 轮换提醒），
     * 配合系统提示词四阶段段序约束，防止面试官全程恋战第一个话题。</p>
     *
     * @param skip        本题是否被跳过
     * @param doneRounds  已问大问题数
     * @param remainMin   剩余分钟（可为负）
     * @return 指令文本
     */
    public static String buildTurnDirective(boolean skip, int doneRounds, long remainMin) {
        String skipNote = skip
                ? "候选人刚刚选择跳过本题（未作答），请简短带过、不做追问，自然转入下一个方向。"
                : "请以面试官身份回应候选人的回答：先一两句简要反馈，再自然提出你的下一个问题或针对性追问。";
        String timeNote;
        if (remainMin <= 0) {
            timeNote = "（本场面试时间已到，请以面试官身份做简短收尾致谢。）";
        } else if (remainMin <= 2) {
            timeNote = "（本场面试临近结束，请在当前话题自然收口，不再展开新的考察方向。）";
        } else {
            timeNote = "（本场面试剩余约 " + remainMin + " 分钟，可自主把握提问节奏与深度。）";
        }
        // 分阶段引导（v13.62）：按轮次推进提醒面试官切换考察阶段/方向
        String phaseNote = "";
        if (doneRounds >= 2 && doneRounds <= 3) {
            phaseNote = "（简历与自我介绍深挖应接近尾声，请转入预热计划中的专业技术考察方向。）";
        } else if (doneRounds >= 4) {
            phaseNote = "（注意考察方向轮换：若当前话题已连续追问 2 轮以上且验证充分，请果断切换下一个方向，不要恋战。）";
        }
        return skipNote
                + "只输出面试官会说的话，不要任何分析、标记或多余格式。"
                + "（本场已问 " + doneRounds + " 个大问题）" + timeNote + phaseNote;
    }

    /**
     * 预热考察计划段落渲染（把 warmup 产出的画像/计划拼进系统提示词）。
     *
     * @param sb  目标 StringBuilder
     * @param arr 计划数组节点（非数组/空则不追加）
     * @param label 段落标签
     */
    public static void appendPlanList(StringBuilder sb, String label, JsonNode arr) {
        if (arr == null || !arr.isArray() || arr.isEmpty()) {
            return;
        }
        List<String> items = new ArrayList<>();
        for (JsonNode n : arr) {
            String t = n.asText("").trim();
            if (StringUtils.isNotEmpty(t)) {
                items.add(t);
            }
        }
        if (!items.isEmpty()) {
            sb.append("- ").append(label).append("：").append(String.join("；", items)).append("\n");
        }
    }

    // ==================== VO 映射 ====================

    /**
     * QA 实体 → 前端 VO（纯字段搬运，无业务判断）。
     *
     * @param qa 问答实体（不可为 null）
     * @return QA VO
     */
    public static com.moyun.ext.cms.domain.vo.VoiceInterviewQaVO toQaVO(PortalVoiceInterviewQA qa) {
        com.moyun.ext.cms.domain.vo.VoiceInterviewQaVO vo =
                new com.moyun.ext.cms.domain.vo.VoiceInterviewQaVO();
        vo.setId(qa.getId());
        vo.setInterviewId(qa.getInterviewId());
        vo.setQuestionId(qa.getQuestionId());
        vo.setQuestionSource(qa.getQuestionSource());
        vo.setQuestionIdx(qa.getQuestionIdx());
        vo.setParentQaId(qa.getParentQaId());
        vo.setQuestion(qa.getQuestion());
        vo.setUserAnswer(qa.getUserAnswer());
        vo.setTranscriptionEdited(qa.getTranscriptionEdited());
        vo.setAiFeedback(qa.getAiFeedback());
        vo.setSpeakText(qa.getSpeakText());
        vo.setScore(qa.getScore());
        vo.setRuleDimensionsJson(qa.getRuleDimensionsJson());
        vo.setHintUsed(qa.getHintUsed());
        vo.setLatencyMs(qa.getLatencyMs());
        vo.setNextAction(qa.getNextAction());
        vo.setCreateTime(qa.getCreateTime());
        return vo;
    }
}
