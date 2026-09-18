package com.moyun.ext.aiapp.handler.impl;

import com.moyun.ext.ai.entity.AiSceneConfig;
import com.moyun.ext.ai.enums.AiSceneEnum;
import com.moyun.ext.aiapp.constant.AiErrorCodes;
import com.moyun.ext.aiapp.handler.AbstractAiSceneHandler;
import com.moyun.ext.aiapp.model.AiExecuteRequest;
import com.moyun.ext.aiapp.model.AiExecuteResponse;
import com.moyun.ext.aiapp.model.data.GenericSceneData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 写作主题生成场景Handler（scene = writing_prompt）
 *
 * <p>从 CmsWritingPromptServiceImpl 直调 LLM 收编。输入：date + specialDates。
 * 输出：title/category/description。AI 不可用时 Service 侧走内置主题池兜底。</p>
 *
 * @author laomao
 * @since 2026-09-18
 */
@Slf4j
@Component("writingPromptHandler")
public class WritingPromptHandler extends AbstractAiSceneHandler {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy年M月d日");
    private static final List<String> WEEK_CN = Arrays.asList("一", "二", "三", "四", "五", "六", "日");
    private static final Pattern TITLE_PATTERN = Pattern.compile("标题[:：]\\s*(.+)");
    private static final Pattern CATEGORY_PATTERN = Pattern.compile("分类[:：]\\s*(.+)");
    private static final Pattern DESC_PATTERN = Pattern.compile("描述[:：]\\s*([\\s\\S]+)");

    @Override
    public String getSceneCode() {
        return AiSceneEnum.WRITING_PROMPT.getCode();
    }

    @Override
    public void validate(AiExecuteRequest request) {
        // date 可选，全参数可选
    }

    @Override
    public AiExecuteResponse<?> execute(AiExecuteRequest request, AiSceneConfig config) {
        String dateStr = getInputString(request, "date");
        LocalDate date = dateStr != null && !dateStr.isBlank()
                ? LocalDate.parse(dateStr) : LocalDate.now();
        String specialDatesStr = getInputString(request, "specialDates");
        List<String> specialDates = specialDatesStr != null && !specialDatesStr.isBlank()
                ? Arrays.asList(specialDatesStr.split("[,，]")) : List.of();

        String systemPrompt = "你是社区写作平台的编辑，负责为每天设计一个\"今日写作主题\"，"
                + "激励创作者写出真实、有感染力的文章。\n"
                + "要求：\n"
                + "1. 主题必须具体、可写，能激发真实表达，避免\"人生\",\"梦想\"等大词。\n"
                + "2. 标题在 30 字以内，简洁有力，吸引点击。\n"
                + "3. 描述在 100 字以内，必须包含写作切入点。\n"
                + "4. 分类严格限定为以下之一：生活/职场/情感/虚构/哲思。\n"
                + "严格按如下三行格式输出，不要任何多余内容：\n"
                + "标题：xxx\n分类：xx\n描述：xxx";

        StringBuilder user = new StringBuilder();
        user.append("今天是").append(date.format(DATE_FMT))
                .append("，星期").append(WEEK_CN.get(date.getDayOfWeek().getValue() - 1)).append("。\n");
        if (!specialDates.isEmpty()) {
            user.append("今天恰逢：").append(String.join("、", specialDates))
                    .append("。请将主题与这个特殊日子自然关联。");
        } else {
            user.append("今天不是特别的节日，请以当前的【节气/季节/自然物候】为核心素材来设计主题。");
        }

        String raw = chat(getSceneCode(), systemPrompt, user.toString());
        if (raw == null || raw.isBlank()) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_CALL_FAILED, "AI服务暂不可用");
        }

        String title = extract(TITLE_PATTERN, raw);
        String category = normalizeCategory(extract(CATEGORY_PATTERN, raw));
        String description = extract(DESC_PATTERN, raw);
        if (title == null || title.isBlank() || description == null || description.isBlank()) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_PARSE_ERROR, "AI输出格式解析失败");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("title", title.length() > 128 ? title.substring(0, 128) : title);
        result.put("category", category);
        result.put("description", description.trim());

        GenericSceneData data = new GenericSceneData();
        data.setStructured(result);
        return AiExecuteResponse.success(data);
    }

    private String extract(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }

    private String normalizeCategory(String category) {
        if (category == null) return "生活";
        List<String> standard = Arrays.asList("生活", "职场", "情感", "虚构", "哲思");
        String c = category.trim();
        return standard.contains(c) ? c : "生活";
    }
}
