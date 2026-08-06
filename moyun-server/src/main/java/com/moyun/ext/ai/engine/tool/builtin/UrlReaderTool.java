package com.moyun.ext.ai.engine.tool.builtin;

import com.moyun.ext.ai.engine.tool.ToolContext;
import com.moyun.ext.ai.engine.tool.ToolExecutor;
import com.moyun.ext.ai.engine.tool.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 网页读取工具
 *
 * <p>读取指定URL的网页内容，提取主要文本信息</p>
 *
 * @author laomao
 */
@Slf4j
@Component
public class UrlReaderTool implements ToolExecutor {

    private static final int MAX_CONTENT_LENGTH = 3000;
    private static final int TIMEOUT_MS = 10000;

    @Override
    public String getName() {
        return "url_reader";
    }

    @Override
    public String getDescription() {
        return "读取指定URL的网页内容，提取主要文本信息";
    }

    @Override
    public String getParametersSchema() {
        return """
            {
                "type": "object",
                "properties": {
                    "url": {
                        "type": "string",
                        "description": "要读取的网页URL"
                    }
                },
                "required": ["url"]
            }
            """;
    }

    @Override
    public ToolResult execute(ToolContext context, Map<String, Object> params) {
        String url = asString(params, "url");

        if (url == null || url.trim().isEmpty()) {
            return ToolResult.fail("URL不能为空");
        }

        // 补全协议
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        try {
            log.info("🌐 读取网页: {}", url);

            Document doc = Jsoup.connect(url)
                    .timeout(TIMEOUT_MS)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .get();

            // 提取标题
            String title = doc.title();

            // 移除脚本和样式
            doc.select("script, style, nav, footer, header, aside").remove();

            // 提取正文内容
            String content = extractMainContent(doc);

            // 限制长度
            if (content.length() > MAX_CONTENT_LENGTH) {
                content = content.substring(0, MAX_CONTENT_LENGTH) + "...\n\n（内容已截断）";
            }

            StringBuilder result = new StringBuilder();
            result.append(String.format("📄 网页标题：%s\n", title));
            result.append(String.format("🔗 URL：%s\n\n", url));
            result.append("📝 正文内容：\n");
            result.append(content);

            log.info("✅ 网页读取成功，内容长度: {}", content.length());
            return ToolResult.success(result.toString());

        } catch (Exception e) {
            log.error("网页读取失败: {}", url, e);
            return ToolResult.fail("网页读取失败: " + e.getMessage());
        }
    }

    /**
     * 提取网页主要内容
     */
    private String extractMainContent(Document doc) {
        // 尝试找到主要内容区域
        String[] mainSelectors = {"article", "main", ".content", ".article", "#content", "#main"};

        for (String selector : mainSelectors) {
            Elements elements = doc.select(selector);
            if (!elements.isEmpty()) {
                return cleanText(elements.first().text());
            }
        }

        // 如果没找到，提取body中的段落
        Elements paragraphs = doc.select("p");
        if (!paragraphs.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Element p : paragraphs) {
                String text = p.text().trim();
                if (text.length() > 20) { // 过滤太短的段落
                    sb.append(text).append("\n\n");
                }
            }
            return cleanText(sb.toString());
        }

        // 最后使用body文本
        return cleanText(doc.body().text());
    }

    /**
     * 清理文本
     */
    private String cleanText(String text) {
        return text
                .replaceAll("\\s+", " ")  // 合并空白
                .replaceAll("\\n{3,}", "\n\n")  // 合并多余换行
                .trim();
    }

    @Override
    public int getTimeoutSeconds() {
        return 15;
    }
}
