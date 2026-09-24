package com.moyun.ext.ai.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

/**
 * AI 场景注册表
 *
 * <p>场景代码的唯一权威来源：业务代码引用、后台管理页总览、绑定保存校验共用。
 * 场景元数据（核心能力/输入/输出）是规格描述，低频变更，随代码走版本管理；
 * 场景与 Agent/模型/知识库/工具/工作流的绑定关系仍在 ai_scene_config 表（支持多版本灰度）。
 *
 * <p><strong>新增场景三步（2B.6 收敛后口径，不再写 Handler）</strong>：
 * 1）本枚举加一个主场景值（task 子任务用 {@link AiSceneTasks} 字符串常量，不膨胀枚举）；
 * 2）ai_scene_config 插配置行（主场景行 + 按需 scene:task 全码行，提示词走
 * user_prompt_template，外部不可信数据用 {{data:标签|key}} 数据通道）；
 * 3）业务 Service 组装 input 调 {@code AiSceneJsonClient}（或网关会话流式通道）。
 * 仅 output_schema 表达力不足的复杂场景才实现 AiSceneHandler（SPI 逃生舱保留）。
 *
 * @author moyun
 */
@Getter
public enum AiSceneEnum {

    VOICE_INTERVIEW("voice_interview", "AI 语音面试",
            "6阶段面试流程 + 智能评分",
            "岗位画像 + 简历上下文 + 面试配置",
            "面试报告（评分/维度/知识点/改进建议）"),

    RESUME_PARSE("resume_parse", "简历解析",
            "简历文本结构化提取",
            "简历原文（文本/OCR 结果）",
            "结构化简历 JSON（基本信息/教育/工作/项目/技能）"),

    RESUME_OPTIMIZE("resume_optimize", "简历优化",
            "岗位匹配 + 深度优化建议",
            "简历内容 + 目标岗位",
            "优化建议 + 对比视图"),

    QUESTION_GENERATE("question_generate", "智能出题",
            "基于知识点生成面试题",
            "岗位 + 技能标签 + 难度",
            "面试题单"),

    FINANCE_ANALYSIS("finance_analysis", "AI 财务分析",
            "多维指标聚合 + 财务健康评分 + LLM 综述",
            "记账流水（月/3月/6月/12月窗口）+ 用户画像",
            "财务分析报告（指标/风险/建议/综述）"),

    SENSITIVE_WORD("sensitive_word", "敏感词检测",
            "文本敏感词识别与风险分级",
            "待检测文本",
            "风险等级 + 命中词列表"),

    DAILY_TOPIC("daily_topic", "今日主题",
            "每日主题生成",
            "日期 + 用户偏好",
            "主题文案 + 配图建议"),

    ARTICLE_META("article_meta", "文章元信息",
            "摘要 / SEO 标题 / SEO 描述 / 关键词",
            "文章标题 + 正文纯文本",
            "summary/seoTitle/seoDescription/seoKeywords"),

    CONTENT_TAGS("content_tags", "内容标签",
            "内容标签提取（3~8 个）",
            "标题 + 正文纯文本",
            "tags 标签列表"),

    WRITING_PROMPT("writing_prompt", "写作主题",
            "每日写作主题生成",
            "日期 + 特殊日期上下文",
            "标题/分类/描述");

    /** 场景代码（数据库 ai_scene_config.scene_code） */
    private final String code;
    /** 场景名称 */
    private final String name;
    /** 核心能力 */
    private final String capability;
    /** 输入 */
    private final String input;
    /** 输出 */
    private final String output;

    AiSceneEnum(String code, String name, String capability, String input, String output) {
        this.code = code;
        this.name = name;
        this.capability = capability;
        this.input = input;
        this.output = output;
    }

    /** 是否已注册的场景代码 */
    public static boolean isRegistered(String code) {
        return Arrays.stream(values()).anyMatch(s -> s.code.equals(code));
    }

    /** 按代码取枚举（未注册返回 null） */
    public static AiSceneEnum of(String code) {
        return Arrays.stream(values()).filter(s -> s.code.equals(code)).findFirst().orElse(null);
    }

    /** 注册表列表（管理页总览用），顺序即枚举声明顺序 */
    public static List<Map<String, Object>> registry() {
        return Arrays.stream(values()).map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", s.code);
            m.put("name", s.name);
            m.put("capability", s.capability);
            m.put("input", s.input);
            m.put("output", s.output);
            return m;
        }).collect(Collectors.toList());
    }
}