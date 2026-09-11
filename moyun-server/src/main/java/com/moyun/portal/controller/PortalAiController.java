package com.moyun.portal.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.ai.service.LLMService;
import com.moyun.util.string.StringUtils;

/**
 * 门户 AI 内容分析统一 Controller（需登录，消耗 AI Token 的能力不放公开接口）
 *
 * <p>设计：一个端点 {@code POST /portal/ai/analyze} + 场景注册表（scene）。
 * 各业务方按 scene 取用分析能力，新增场景只需在 {@link #SCENES} 注册一条
 * （提示词模板 + 输出解析 + 本地兜底），无需新增接口。
 *
 * <p>已注册场景：
 * <ul>
 *   <li>article-meta：文章元信息（摘要 / SEO 标题 / SEO 描述 / 关键词），发布页使用</li>
 *   <li>tags：内容标签提取（3~8 个），可用于文章标签、收藏打标等</li>
 * </ul>
 *
 * @author moyun
 */
@Tag(name = "门户 AI 内容分析", description = "统一内容分析入口：按场景返回结构化分析结果，AI 未配置时本地兜底")
@RestController
@RequestMapping("/portal/ai")
public class PortalAiController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(PortalAiController.class);

    /** 送入 LLM 的正文上限（字符），控制 Token 消耗 */
    private static final int MAX_CONTENT_CHARS = 3000;

    /** AI 生成服务（未配置默认模型时为 null，走本地兜底） */
    @Autowired(required = false)
    private LLMService llmService;

    // ==================== 请求/响应结构 ====================

    /** 统一请求体：场景 + 标题 + 正文（markdown 或 HTML 均可，后端统一转纯文本） */
    public static class AnalyzeQuery {
        /** 分析场景，见 SCENES 注册表 */
        private String scene;
        private String title;
        private String content;

        public String getScene() { return scene; }
        public void setScene(String scene) { this.scene = scene; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    /**
     * 场景处理器：构造提示词 + 解析 AI 输出 + 本地兜底。
     * 新增场景 = 实现一个 SceneHandler 并注册到 SCENES。
     */
    private interface SceneHandler {
        /** 构造 LLM 提示词（plainText 已是纯文本） */
        String buildPrompt(String title, String plainText);

        /** 从 AI 原始输出解析结构化结果；返回 null 表示解析失败（将走兜底） */
        Map<String, Object> parse(String answer, String title, String plainText);

        /** AI 不可用/失败时的本地兜底结果 */
        Map<String, Object> fallback(String title, String plainText);
    }

    // ==================== 场景注册表 ====================

    private static final Map<String, SceneHandler> SCENES = new LinkedHashMap<>();

    static {
        // 场景：文章元信息（摘要 / SEO 标题 / SEO 描述 / 关键词）
        SCENES.put("article-meta", new SceneHandler() {
            private final Pattern summary = Pattern.compile("摘要[:：]\\s*(.+)");
            private final Pattern seoTitle = Pattern.compile("SEO标题[:：]\\s*(.+)");
            private final Pattern seoDesc = Pattern.compile("SEO描述[:：]\\s*(.+)");
            private final Pattern keywords = Pattern.compile("关键词[:：]\\s*(.+)");

            @Override
            public String buildPrompt(String title, String plainText) {
                return "你是内容平台的 SEO 编辑。请根据下面的文章标题和正文，输出文章的摘要与 SEO 信息。\n"
                        + "严格按以下四行格式输出，不要输出其他任何内容（每行标签后跟内容）：\n"
                        + "摘要：100字以内，概括文章核心内容\n"
                        + "SEO标题：60字以内，包含核心关键词，比原标题更利于搜索\n"
                        + "SEO描述：150字以内，吸引点击的搜索结果描述\n"
                        + "关键词：3~6个，用英文逗号分隔，不要带序号\n\n"
                        + "文章标题：" + (StringUtils.isEmpty(title) ? "（无标题）" : title) + "\n"
                        + "文章正文：\n" + clip(plainText, MAX_CONTENT_CHARS);
            }

            @Override
            public Map<String, Object> parse(String answer, String title, String plainText) {
                String s = group(summary, answer);
                if (StringUtils.isEmpty(s)) {
                    return null;
                }
                String st = group(seoTitle, answer);
                String sd = group(seoDesc, answer);
                String kw = group(keywords, answer);
                Map<String, Object> r = new HashMap<>();
                r.put("summary", clip(s, 200));
                r.put("seoTitle", clip(StringUtils.isEmpty(st) ? title : st, 100));
                r.put("seoDescription", clip(StringUtils.isEmpty(sd) ? s : sd, 160));
                r.put("seoKeywords", kw == null ? "" : clip(kw, 100));
                return r;
            }

            @Override
            public Map<String, Object> fallback(String title, String plainText) {
                String head = clip(plainText.replaceAll("\\s+", " ").trim(), 160);
                Map<String, Object> r = new HashMap<>();
                r.put("summary", head);
                r.put("seoTitle", clip(title, 100));
                r.put("seoDescription", clip(StringUtils.isEmpty(head) ? title : head, 160));
                r.put("seoKeywords", "");
                return r;
            }
        });

        // 场景：内容标签提取
        SCENES.put("tags", new SceneHandler() {
            @Override
            public String buildPrompt(String title, String plainText) {
                return "你是内容平台的编辑。请根据下面的内容提取 3~8 个最贴切的主题标签。\n"
                        + "严格只输出一行，标签之间用英文逗号分隔，不要带序号和其他文字。示例：职场,成长,方法论\n\n"
                        + "标题：" + (StringUtils.isEmpty(title) ? "（无标题）" : title) + "\n"
                        + "内容：\n" + clip(plainText, MAX_CONTENT_CHARS);
            }

            @Override
            public Map<String, Object> parse(String answer, String title, String plainText) {
                if (StringUtils.isEmpty(answer)) {
                    return null;
                }
                // 取第一行，去掉可能的引号与多余符号
                String line = answer.trim().split("\n")[0].replaceAll("[\"'“”]", "");
                String[] parts = line.split("[,，]");
                List<String> tags = Arrays.stream(parts)
                        .map(String::trim)
                        .filter(t -> !t.isEmpty() && t.length() <= 20)
                        .limit(8)
                        .toList();
                if (tags.isEmpty()) {
                    return null;
                }
                Map<String, Object> r = new HashMap<>();
                r.put("tags", tags);
                return r;
            }

            @Override
            public Map<String, Object> fallback(String title, String plainText) {
                Map<String, Object> r = new HashMap<>();
                r.put("tags", List.of());
                return r;
            }
        });
    }

    // ==================== 统一入口 ====================

    @Operation(summary = "内容分析（统一入口）",
            description = "按场景分析标题+正文，返回结构化结果。scene: article-meta=文章元信息(摘要/SEO/关键词)，tags=标签提取。AI 未配置时返回本地兜底结果（source=fallback）")
    @PostMapping("/analyze")
    public AjaxResult analyze(@RequestBody AnalyzeQuery query) {
        String scene = query.getScene();
        SceneHandler handler = SCENES.get(scene);
        if (handler == null) {
            return error("不支持的分析场景: " + scene + "，可用: " + SCENES.keySet());
        }

        String title = query.getTitle() == null ? "" : query.getTitle().trim();
        String plainText = toPlainText(query.getContent());

        Map<String, Object> result;
        String source = "fallback";

        if (llmService != null && StringUtils.isNotEmpty(plainText)) {
            try {
                String answer = llmService.generate(handler.buildPrompt(title, plainText));
                Map<String, Object> parsed = handler.parse(answer, title, plainText);
                if (parsed != null) {
                    result = parsed;
                    source = "ai";
                } else {
                    log.warn("[PortalAi] scene={} AI 输出解析失败，走本地兜底 answer={}", scene, clip(answer, 120));
                    result = handler.fallback(title, plainText);
                }
            } catch (Exception e) {
                log.warn("[PortalAi] scene={} AI 生成失败，走本地兜底 err={}", scene, e.getMessage());
                result = handler.fallback(title, plainText);
            }
        } else {
            if (llmService == null) {
                log.warn("[PortalAi] LLMService 未注入（AI 模块未启用），scene={} 走本地兜底", scene);
            }
            result = handler.fallback(title, plainText);
        }

        result.put("source", source);
        return AjaxResult.success(result);
    }

    // ==================== 通用工具 ====================

    /** HTML/Markdown 统一转纯文本（去标签、去 markdown 记号、压缩空白） */
    private String toPlainText(String content) {
        if (StringUtils.isEmpty(content)) {
            return "";
        }
        return content
                .replaceAll("(?s)<script.*?</script>", "")
                .replaceAll("(?s)<style.*?</style>", "")
                .replaceAll("(?s)<!--.*?-->", "")
                .replaceAll("<[^>]+>", " ")               // HTML 标签
                .replaceAll("!\\[.*?]\\(.*?\\)", " ")      // markdown 图片
                .replaceAll("\\[([^]]+)]\\(.*?\\)", "$1")  // markdown 链接保留文字
                .replaceAll("[#>*`~|_-]{1,}", " ")         // markdown 记号
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String group(Pattern pattern, String text) {
        if (StringUtils.isEmpty(text)) {
            return null;
        }
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }

    private static String clip(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
