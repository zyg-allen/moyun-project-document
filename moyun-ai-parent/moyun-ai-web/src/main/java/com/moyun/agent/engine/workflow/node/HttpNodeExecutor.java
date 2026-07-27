package com.moyun.agent.engine.workflow.node;

import com.fasterxml.jackson.core.type.TypeReference;
import com.moyun.agent.util.JsonUtils;
import com.moyun.agent.engine.workflow.WorkflowContext;
import com.moyun.agent.engine.workflow.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTTP 请求节点执行器
 *
 * @author laomao
 */
@Slf4j
@Component
public class HttpNodeExecutor extends BaseNodeExecutor {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getType() {
        return "http";
    }

    @Override
    public NodeResult execute(WorkflowNode node, WorkflowContext context) {
        Map<String, Object> config = node.getConfig();
        if (config == null) {
            return NodeResult.fail("HTTP节点配置为空");
        }

        try {
            // 获取配置
            String url = (String) config.get("url");
            String method = (String) config.getOrDefault("method", "GET");
            String outputVariable = (String) config.getOrDefault("outputVariable", "http_response");

            // 获取请求头 - 优先使用 headersJson
            Map<String, String> headers = new HashMap<>();
            String headersJson = (String) config.get("headersJson");
            if (headersJson != null && !headersJson.trim().isEmpty()) {
                headersJson = replaceVariables(headersJson, context);
                Map<String, String> parsedHeaders = JsonUtils.fromJson(headersJson, new TypeReference<Map<String, String>>() {});
                if (parsedHeaders != null) {
                    headers = parsedHeaders;
                }
            }
            if (headers.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, String> configHeaders = (Map<String, String>) config.getOrDefault("headers", new HashMap<>());
                headers = configHeaders;
            }

            // 获取请求体
            Object body = config.get("body");

            if (url == null || url.isEmpty()) {
                return NodeResult.fail("URL为空");
            }

            // 替换变量
            url = replaceVariables(url, context);
            if (body instanceof String) {
                body = replaceVariables((String) body, context);
            }

            log.info("🌐 HTTP节点执行: {} {}", method, url);

            // 构建请求头
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpHeaders.add(entry.getKey(), replaceVariables(entry.getValue(), context));
            }

            // 发送请求
            HttpEntity<Object> requestEntity = new HttpEntity<>(body, httpHeaders);
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.valueOf(method.toUpperCase()),
                    requestEntity,
                    String.class
            );

            String responseBody = response.getBody();
            log.info("🌐 HTTP响应: status={}, body={}", response.getStatusCode(),
                    responseBody != null && responseBody.length() > 200 ?
                            responseBody.substring(0, 200) + "..." : responseBody);

            // 尝试解析为JSON
            Object result = JsonUtils.fromJson(responseBody, Object.class);
            if (result == null) {
                result = responseBody;
            }

            context.setVariable(outputVariable, result);
            return NodeResult.success(result);

        } catch (Exception e) {
            log.error("HTTP节点执行失败", e);
            return NodeResult.fail("HTTP请求失败: " + e.getMessage());
        }
    }

    // replaceVariables方法已移至BaseNodeExecutor
}
