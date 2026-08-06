package com.moyun.ext.ai.engine.tool.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.ai.engine.tool.ToolContext;
import com.moyun.ext.ai.engine.tool.ToolExecutor;
import com.moyun.ext.ai.engine.tool.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 网络搜索工具
 *
 * <p>搜索互联网获取最新信息</p>
 *
 * @author laomao
 */
@Slf4j
@Component
public class WebSearchTool implements ToolExecutor {

    @Value("${tool.search.api-key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "web_search";
    }

    @Override
    public String getDescription() {
        return "搜索互联网获取最新信息，适用于查询新闻、事件、知识等实时内容";
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

        // 如果没有配置API Key，返回提示
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("搜索API未配置，返回模拟结果");
            return getMockSearchResult(query);
        }

        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = String.format(
                    "https://api.bing.microsoft.com/v7.0/search?q=%s&count=%d&mkt=zh-CN",
                    encodedQuery, count
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("Ocp-Apim-Subscription-Key", apiKey);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            return parseSearchResponse(response.getBody(), query);

        } catch (Exception e) {
            log.error("网络搜索失败: {}", query, e);
            return getMockSearchResult(query);
        }
    }

    /**
     * 解析搜索响应
     */
    private ToolResult parseSearchResponse(String response, String query) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode webPages = root.path("webPages").path("value");

            if (webPages.isEmpty()) {
                return ToolResult.success("未找到相关搜索结果");
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("🔍 搜索「%s」的结果：\n\n", query));

            int index = 1;
            for (JsonNode page : webPages) {
                String name = page.path("name").asText();
                String snippet = page.path("snippet").asText();
                String url = page.path("url").asText();

                sb.append(String.format("%d. **%s**\n", index++, name));
                sb.append(String.format("   %s\n", snippet));
                sb.append(String.format("   🔗 %s\n\n", url));
            }

            return ToolResult.success(sb.toString().trim());

        } catch (Exception e) {
            log.error("解析搜索结果失败", e);
            return ToolResult.fail("解析搜索结果失败");
        }
    }

    /**
     * 返回模拟搜索结果
     */
    private ToolResult getMockSearchResult(String query) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("🔍 搜索「%s」的结果（模拟数据）：\n\n", query));
        sb.append("⚠️ 搜索API未配置，无法获取真实搜索结果。\n\n");
        sb.append("请配置 Bing Search API 或其他搜索服务来启用此功能。\n");
        sb.append("配置方式：在 application.properties 中设置 tool.search.api-key");

        return ToolResult.success(sb.toString());
    }
}
