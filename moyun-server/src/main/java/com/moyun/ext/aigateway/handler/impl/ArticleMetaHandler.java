package com.moyun.ext.aigateway.handler.impl;

import com.moyun.ext.ai.entity.AiSceneConfig;
import com.moyun.ext.ai.enums.AiSceneEnum;
import com.moyun.ext.aigateway.constant.AiErrorCodes;
import com.moyun.ext.aigateway.handler.AbstractAiSceneHandler;
import com.moyun.ext.aigateway.model.AiExecuteRequest;
import com.moyun.ext.aigateway.model.AiExecuteResponse;
import com.moyun.ext.aigateway.model.data.GenericSceneData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文章元信息场景Handler（scene = article_meta）
 *
 * <p>从 PortalAiController 自造 SCENES 收编。输入：title + content（纯文本）。
 * 输出：summary/seoTitle/seoDescription/seoKeywords。</p>
 *
 * @author laomao
 * @since 2026-09-18
 */
@Slf4j
@Component("articleMetaHandler")
public class ArticleMetaHandler extends AbstractAiSceneHandler {

    private static final int MAX_CONTENT_CHARS = 3000;
    private static final Pattern SUMMARY = Pattern.compile("摘要[:：]\\s*(.+)");
    private static final Pattern SEO_TITLE = Pattern.compile("SEO标题[:：]\\s*(.+)");
    private static final Pattern SEO_DESC = Pattern.compile("SEO描述[:：]\\s*(.+)");
    private static final Pattern KEYWORDS = Pattern.compile("关键词[:：]\\s*(.+)");

    @Override
    public String getSceneCode() {
        return AiSceneEnum.ARTICLE_META.getCode();
    }

    @Override
    public void validate(AiExecuteRequest request) {
        requireInputString(request, "content");
    }

    @Override
    public AiExecuteResponse<?> execute(AiExecuteRequest request, AiSceneConfig config) {
        String title = getInputString(request, "title");
        String content = requireInputString(request, "content");

        String systemPrompt = "你是内容平台的 SEO 编辑。请根据下面的文章标题和正文，输出文章的摘要与 SEO 信息。\n"
                + "严格按以下四行格式输出，不要输出其他任何内容（每行标签后跟内容）：\n"
                + "摘要：100字以内，概括文章核心内容\n"
                + "SEO标题：60字以内，包含核心关键词，比原标题更利于搜索\n"
                + "SEO描述：150字以内，吸引点击的搜索结果描述\n"
                + "关键词：3~6个，用英文逗号分隔，不要带序号";

        StringBuilder user = new StringBuilder();
        user.append("文章标题：").append(title != null ? title : "（无标题）").append("\n");
        user.append("文章正文：\n").append(clip(content, MAX_CONTENT_CHARS));

        String raw = chat(getSceneCode(), systemPrompt, user.toString());
        if (raw == null || raw.isBlank()) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_CALL_FAILED, "AI服务暂不可用");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("summary", clip(group(SUMMARY, raw), 200));
        result.put("seoTitle", clip(orDefault(group(SEO_TITLE, raw), title), 100));
        result.put("seoDescription", clip(orDefault(group(SEO_DESC, raw), group(SUMMARY, raw)), 160));
        result.put("seoKeywords", orDefault(group(KEYWORDS, raw), ""));

        GenericSceneData data = new GenericSceneData();
        data.setStructured(result);
        return AiExecuteResponse.success(data);
    }

    private String group(Pattern pattern, String text) {
        if (text == null || text.isBlank()) return null;
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }

    private String orDefault(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private String clip(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
