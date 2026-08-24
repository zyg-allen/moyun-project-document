package com.moyun.ext.ai.engine.tool.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.ai.engine.tool.ToolContext;
import com.moyun.ext.ai.engine.tool.ToolExecutor;
import com.moyun.ext.ai.engine.tool.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 网络搜索工具
 *
 * <p>搜索互联网获取最新信息（真实数据）。</p>
 *
 * <p>数据通道（按优先级）：</p>
 * <ol>
 *     <li>Bing Search API：配置 tool.search.api-key 时启用</li>
 *     <li>必应网页版（默认）：免费、无需 API Key、国内可达，
 *         抓取 cn.bing.com 搜索结果页并用 jsoup 解析。</li>
 *     <li>DuckDuckGo（备用）：必应网页版失败时降级尝试（国内网络可能不可达）。</li>
 * </ol>
 *
 * @author laomao
 */
@Slf4j
@Component
public class WebSearchTool implements ToolExecutor {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    @Value("${tool.search.api-key:}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RestTemplate restTemplate;

    public WebSearchTool() {
        // 设置超时，避免第三方接口无响应时阻塞工具线程
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public String getName() {
        return "web_search";
    }

    @Override
    public String getDescription() {
        return "搜索互联网获取最新信息（真实数据），适用于查询新闻、事件、知识等实时内容";
    }

    @Override
    public String getParametersSchema() {
        return """
            {
                "type": "object",
                "properties": {
                    "query": {
                        "type": "string",
                        "description": "搜索关键词"
                    },
                    "count": {
                        "type": "integer",
                        "description": "返回结果数量，默认5条",
                        "default": 5
                    }
                },
                "required": ["query"]
            }
            """;
    }

    @Override
    public ToolResult execute(ToolContext context, Map<String, Object> params) {
        String query = asString(params, "query");
        int count = asInt(params, "count", 5);

        if (query == null || query.trim().isEmpty()) {
            return ToolResult.fail("搜索关键词不能为空");
        }
        if (count < 1) count = 1;
        if (count > 10) count = 10;
        query = query.trim();

        // 1. 已配置 Bing API Key 时优先使用
        if (apiKey != null && !apiKey.isEmpty()) {
            try {
                ToolResult result = searchByBingApi(query, count);
                if (result.isSuccess()) {
                    return result;
                }
            } catch (Exception e) {
                log.warn("Bing API 搜索失败，降级到必应网页版: {}", e.getMessage());
            }
        }

        // 2. 必应网页版（免费、国内可达）
        try {
            ToolResult result = searchByBingHtml(query, count);
            if (result.isSuccess()) {
                return result;
            }
        } catch (Exception e) {
            log.warn("必应网页版搜索失败，降级到 DuckDuckGo: {}", e.getMessage());
        }

        // 3. DuckDuckGo 备用
        return searchByDuckDuckGo(query, count);
    }

    /**
     * 必应网页版通道：抓取 cn.bing.com 搜索结果页 + jsoup 解析
     */
    private ToolResult searchByBingHtml(String query, int count) throws Exception {
        Document doc = Jsoup.connect("https://cn.bing.com/search")
                .data("q", query)
                .data("count", String.valueOf(count))
                .userAgent(USER_AGENT)
                .header("Accept-Language", "zh-CN,zh;q=0.9")
                .timeout(15000)
                .get();

        Elements results = doc.select("li.b_algo");

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("🔍 搜索「%s」的结果：\n\n", query));

        int index = 1;
        for (Element result : results) {
            if (index > count) break;

            Element link = result.selectFirst("h2 a");
            if (link == null) continue;

            String title = link.text();
            String url = link.attr("href");
            Element snippetEl = result.selectFirst("div.b_caption p");
            if (snippetEl == null) {
                snippetEl = result.selectFirst(".b_caption");
            }
            String snippet = snippetEl != null ? snippetEl.text() : "";

            sb.append(String.format("%d. %s\n", index++, title));
            if (!snippet.isEmpty()) {
                sb.append(String.format("   %s\n", snippet));
            }
            sb.append(String.format("   🔗 %s\n\n", url));
        }

        if (index == 1) {
            return ToolResult.fail("必应网页版未解析到结果");
        }

        sb.append("（数据来源：必应）");
        return ToolResult.success(sb.toString().trim());
    }

    /**
     * Bing Search API 通道（需配置 tool.search.api-key）
     */
    private ToolResult searchByBingApi(String query, int count) throws Exception {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = String.format(
                "https://api.bing.microsoft.com/v7.0/search?q=%s&count=%d&mkt=zh-CN",
                encodedQuery, count
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Ocp-Apim-Subscription-Key", apiKey);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        // 注意：传 URI 对象避免 String URL 被二次编码（% → %25），导致中文关键词乱码
        ResponseEntity<String> response = restTemplate.exchange(URI.create(url), HttpMethod.GET, entity, String.class);
        return parseBingApiResponse(response.getBody(), query);
    }

    /**
     * DuckDuckGo 备用通道：HTML 端点搜索 + jsoup 解析结果（国内网络可能不可达）
     */
    private ToolResult searchByDuckDuckGo(String query, int count) {
        try {
            Document doc = Jsoup.connect("https://html.duckduckgo.com/html/")
                    .data("q", query)
                    .userAgent(USER_AGENT)
                    .timeout(15000)
                    .post();

            Elements results = doc.select("div.result");
            if (results.isEmpty()) {
                results = doc.select("div.web-result");
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("🔍 搜索「%s」的结果：\n\n", query));

            int index = 1;
            for (Element result : results) {
                if (index > count) break;

                Element link = result.selectFirst("a.result__a");
                if (link == null) continue;

                String title = link.text();
                String url = resolveDdgUrl(link.attr("href"));
                Element snippetEl = result.selectFirst(".result__snippet");
                String snippet = snippetEl != null ? snippetEl.text() : "";

                sb.append(String.format("%d. %s\n", index++, title));
                if (!snippet.isEmpty()) {
                    sb.append(String.format("   %s\n", snippet));
                }
                sb.append(String.format("   🔗 %s\n\n", url));
            }

            if (index == 1) {
                return ToolResult.success("未找到相关搜索结果");
            }

            sb.append("（数据来源：DuckDuckGo）");
            return ToolResult.success(sb.toString().trim());

        } catch (Exception e) {
            log.error("DuckDuckGo 搜索失败: {}", query, e);
            return ToolResult.fail("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 解析 DuckDuckGo 跳转链接，还原真实 URL
     * 形如 //duckduckgo.com/l/?uddg=https%3A%2F%2Fexample.com%2F&rut=xxx
     */
    private String resolveDdgUrl(String href) {
        if (href == null || href.isEmpty()) {
            return "";
        }
        if (href.startsWith("//")) {
            href = "https:" + href;
        }
        if (href.contains("uddg=")) {
            int start = href.indexOf("uddg=") + 5;
            int end = href.indexOf('&', start);
            String encoded = end > start ? href.substring(start, end) : href.substring(start);
            try {
                return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
            } catch (Exception ignored) {
            }
        }
        return href;
    }

    /**
     * 解析 Bing API 搜索响应
     */
    private ToolResult parseBingApiResponse(String response, String query) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        JsonNode webPages = root.path("webPages").path("value");

        if (webPages.isEmpty()) {
            return ToolResult.fail("Bing API 未返回结果");
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("🔍 搜索「%s」的结果：\n\n", query));

        int index = 1;
        for (JsonNode page : webPages) {
            String name = page.path("name").asText();
            String snippet = page.path("snippet").asText();
            String url = page.path("url").asText();

            sb.append(String.format("%d. %s\n", index++, name));
            sb.append(String.format("   %s\n", snippet));
            sb.append(String.format("   🔗 %s\n\n", url));
        }

        sb.append("（数据来源：Bing API）");
        return ToolResult.success(sb.toString().trim());
    }
}
