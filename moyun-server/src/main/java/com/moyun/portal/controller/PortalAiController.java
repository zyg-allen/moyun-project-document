package com.moyun.portal.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.moyun.ext.ai.enums.AiSceneEnum;
import com.moyun.ext.aigateway.constant.AiErrorCodes;
import com.moyun.ext.aigateway.model.AiExecuteRequest;
import com.moyun.ext.aigateway.model.AiExecuteResponse;
import com.moyun.ext.aigateway.model.data.GenericSceneData;
import com.moyun.ext.aigateway.service.AiGatewayService;
import com.moyun.util.string.StringUtils;

/**
 * 门户 AI 内容分析统一 Controller（需登录，消耗 AI Token 的能力不放公开接口）
 *
 * <p>article-meta / tags 两场景均已配置驱动收编（ai_scene_config 配置行 +
 * DefaultSceneExecutor 执行，2B.2/2B.3），本类只做标题/正文组装与本地兜底。</p>
 *
 * <p>设计：一个端点 {@code POST /portal/ai/analyze}，按 scene 映射网关场景码，
 * 标题/正文统一转纯文本后走 {@link AiGatewayService}，AI 未配置/失败时本地兜底。</p>
 *
 * <p>已支持场景：
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
    private AiGatewayService aiGatewayService;

    // ==================== 请求结构 ====================

    /** 统一请求体：场景 + 标题 + 正文（markdown 或 HTML 均可，后端统一转纯文本） */
    public static class AnalyzeQuery {
        /** 分析场景：article-meta=文章元信息，tags=标签提取 */
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

    // ==================== 统一入口 ====================

    @Operation(summary = "内容分析（统一入口）",
            description = "按场景分析标题+正文，返回结构化结果。scene: article-meta=文章元信息(摘要/SEO/关键词)，tags=标签提取。AI 未配置时返回本地兜底结果（source=fallback）")
    @PostMapping("/analyze")
    public AjaxResult analyze(@RequestBody AnalyzeQuery query) {
        String scene = query.getScene();
        String sceneCode = mapSceneCode(scene);
        if (sceneCode == null) {
            return error("不支持的分析场景: " + scene);
        }

        String title = query.getTitle() == null ? "" : query.getTitle().trim();
        String plainText = toPlainText(query.getContent());

        Map<String, Object> result;
        String source = "fallback";

        if (aiGatewayService != null && StringUtils.isNotEmpty(plainText)) {
            try {
                Map<String, Object> input = new HashMap<>();
                input.put("title", title);
                // 正文截断下沉到调用方（配置行模板不含截断逻辑）
                input.put("content", clip(plainText, MAX_CONTENT_CHARS));
                AiExecuteRequest request = new AiExecuteRequest();
                request.setSceneCode(sceneCode);
                request.setInput(input);
                AiExecuteResponse<?> resp = aiGatewayService.execute(request);
                if (resp.getCode() != null && resp.getCode() == AiErrorCodes.SUCCESS
                        && resp.getData() instanceof GenericSceneData generic
                        && generic.getStructured() != null) {
                    result = new HashMap<>(generic.getStructured());
                    source = "ai";
                } else {
                    result = localFallback(scene, title, plainText);
                }
            } catch (Exception e) {
                log.warn("[PortalAi] scene={} AI 生成失败，走本地兜底 err={}", scene, e.getMessage());
                result = localFallback(scene, title, plainText);
            }
        } else {
            if (aiGatewayService == null) {
                log.warn("[PortalAi] AiGatewayService 未注入，scene={} 走本地兜底", scene);
            }
            result = localFallback(scene, title, plainText);
        }

        result.put("source", source);
        return AjaxResult.success(result);
    }

    // ==================== 场景映射 + 本地兜底 ====================

    private String mapSceneCode(String scene) {
        if ("article-meta".equals(scene)) return AiSceneEnum.ARTICLE_META.getCode();
        if ("tags".equals(scene)) return AiSceneEnum.CONTENT_TAGS.getCode();
        return null;
    }

    private Map<String, Object> localFallback(String scene, String title, String plainText) {
        if ("article-meta".equals(scene)) {
            String head = clip(plainText.replaceAll("\\s+", " ").trim(), 160);
            Map<String, Object> r = new HashMap<>();
            r.put("summary", head);
            r.put("seoTitle", clip(title, 100));
            r.put("seoDescription", clip(StringUtils.isEmpty(head) ? title : head, 160));
            r.put("seoKeywords", "");
            return r;
        }
        if ("tags".equals(scene)) {
            Map<String, Object> r = new HashMap<>();
            r.put("tags", List.of());
            return r;
        }
        return new HashMap<>();
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

    private static String clip(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
