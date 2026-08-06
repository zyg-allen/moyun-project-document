package com.moyun.ext.ai.engine.workflow.node;

import com.moyun.ext.ai.engine.workflow.WorkflowContext;
import com.moyun.ext.ai.engine.workflow.WorkflowNode;
import com.moyun.ext.ai.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Webhook节点执行器
 *
 * <p>支持发送Webhook通知到外部系统</p>
 * <p>支持的功能：</p>
 * <ul>
 *     <li>发送JSON格式的Webhook</li>
 *     <li>自定义请求头</li>
 *     <li>签名验证</li>
 *     <li>重试机制</li>
 * </ul>
 *
 * @author laomao
 * @since 2025-12-12
 */
@Slf4j
@Component
public class WebhookNodeExecutor extends BaseNodeExecutor {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getType() {
        return "webhook";
    }

    @Override
    public NodeResult execute(WorkflowNode node, WorkflowContext context) {
        Map<String, Object> config = node.getConfig();
        if (config == null) {
            return NodeResult.fail("Webhook节点配置为空");
        }

        try {
            String url = (String) config.get("url");
            String outputVariable = (String) config.getOrDefault("outputVariable", "webhook_result");
            int maxRetries = getIntValue(config.get("maxRetries"), 3);
            int timeout = getIntValue(config.get("timeout"), 30);

            if (url == null || url.trim().isEmpty()) {
                return NodeResult.fail("Webhook URL为空");
            }

            // 替换变量
            url = replaceVariables(url, context);

            // 构建请求体
            Map<String, Object> payload = buildPayload(config, context);

            // 构建请求头
            HttpHeaders headers = buildHeaders(config, context, payload);

            log.info("🔔 Webhook节点执行: url={}", url);

            // 发送请求（带重试）
            Map<String, Object> result = sendWithRetry(url, payload, headers, maxRetries);

            log.info("🔔 Webhook发送完成: success={}", result.get("success"));

            context.setVariable(outputVariable, result);
            return NodeResult.success(result);

        } catch (Exception e) {
            log.error("Webhook节点执行失败", e);
            return NodeResult.fail("Webhook发送失败: " + e.getMessage());
        }
    }

    /**
     * 构建请求体
     */
    private Map<String, Object> buildPayload(Map<String, Object> config, WorkflowContext context) {
        Map<String, Object> payload = new HashMap<>();

        // 添加事件类型
        String eventType = (String) config.getOrDefault("eventType", "workflow.completed");
        payload.put("event", eventType);

        // 添加时间戳
        payload.put("timestamp", System.currentTimeMillis());

        // 添加唯一ID
        payload.put("id", UUID.randomUUID().toString());

        // 添加自定义数据
        String dataJson = (String) config.get("dataJson");
        if (dataJson != null && !dataJson.trim().isEmpty()) {
            dataJson = replaceVariables(dataJson, context);
            try {
                Map<String, Object> customData = JsonUtils.fromJson(dataJson, Map.class);
                if (customData != null) {
                    payload.put("data", customData);
                }
            } catch (Exception e) {
                // 如果不是JSON，直接作为字符串
                payload.put("data", dataJson);
            }
        } else {
            // 默认包含所有上下文变量
            payload.put("data", context.getVariables());
        }

        return payload;
    }

    /**
     * 构建请求头
     */
    private HttpHeaders buildHeaders(Map<String, Object> config, WorkflowContext context, 
                                      Map<String, Object> payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 添加自定义请求头
        String headersJson = (String) config.get("headersJson");
        if (headersJson != null && !headersJson.trim().isEmpty()) {
            headersJson = replaceVariables(headersJson, context);
            try {
                Map<String, String> customHeaders = JsonUtils.fromJson(headersJson, Map.class);
                if (customHeaders != null) {
                    customHeaders.forEach(headers::set);
                }
            } catch (Exception e) {
                log.warn("解析自定义请求头失败: {}", e.getMessage());
            }
        }

        // 添加签名（如果配置了密钥）
        String secret = (String) config.get("secret");
        if (secret != null && !secret.trim().isEmpty()) {
            String signature = generateSignature(payload, secret);
            headers.set("X-Webhook-Signature", signature);
        }

        return headers;
    }

    /**
     * 生成签名
     */
    private String generateSignature(Map<String, Object> payload, String secret) {
        try {
            String payloadJson = JsonUtils.toJson(payload);
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKey = 
                new javax.crypto.spec.SecretKeySpec(secret.getBytes(), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payloadJson.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return "sha256=" + hexString.toString();
        } catch (Exception e) {
            log.warn("生成签名失败: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 发送请求（带重试）
     */
    private Map<String, Object> sendWithRetry(String url, Map<String, Object> payload,
                                               HttpHeaders headers, int maxRetries) {
        Map<String, Object> result = new HashMap<>();
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
                ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, String.class);

                result.put("success", true);
                result.put("statusCode", response.getStatusCodeValue());
                result.put("response", response.getBody());
                result.put("attempt", attempt);
                return result;

            } catch (Exception e) {
                lastException = e;
                log.warn("Webhook发送失败 (尝试 {}/{}): {}", attempt, maxRetries, e.getMessage());

                if (attempt < maxRetries) {
                    try {
                        // 指数退避
                        Thread.sleep(1000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        result.put("success", false);
        result.put("error", lastException != null ? lastException.getMessage() : "未知错误");
        result.put("attempt", maxRetries);
        return result;
    }

    private int getIntValue(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
