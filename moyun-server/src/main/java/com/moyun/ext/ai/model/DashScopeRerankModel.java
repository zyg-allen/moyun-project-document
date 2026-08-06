package com.moyun.ext.ai.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * DashScope Rerank 模型实现
 * 
 * <p>通过 DashScope API 调用 Qwen3-Rerank 或 GTE-Rerank-V2 模型进行文档重排序</p>
 * 
 * <p>API 文档: https://help.aliyun.com/zh/model-studio/developer-reference/text-rerank-api</p>
 * 
 * @author laomao
 * @since 2025-01-22
 */
@Slf4j
public class DashScopeRerankModel implements RerankModel {
    
    private final String apiKey;
    private final String baseUrl;
    private final String modelName;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    /**
     * 构造函数
     * 
     * @param apiKey DashScope API Key
     * @param baseUrl API 基础URL（默认: https://dashscope.aliyuncs.com/api/v1）
     * @param modelName 模型名称（qwen3-rerank 或 gte-rerank-v2）
     */
    public DashScopeRerankModel(String apiKey, String baseUrl, String modelName) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl != null ? baseUrl : "https://dashscope.aliyuncs.com/api/v1";
        this.modelName = modelName;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public List<RerankResult> rerank(String query, List<String> documents, int topK) {
        if (documents == null || documents.isEmpty()) {
            log.warn("⚠️ Rerank 输入文档为空");
            return new ArrayList<>();
        }
        
        if (topK <= 0) {
            topK = documents.size();
        }
        
        log.info("🤖 调用 DashScope Rerank API: model={}, documents={}, topK={}", 
            modelName, documents.size(), topK);
        
        String url = baseUrl + "/services/rerank/text-rerank/text-rerank";
        
        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelName);
            
            Map<String, Object> input = new HashMap<>();
            input.put("query", query);
            input.put("documents", documents);
            requestBody.put("input", input);
            
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("top_n", Math.min(topK, documents.size()));
            parameters.put("return_documents", true);
            requestBody.put("parameters", parameters);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            // 发送请求
            long startTime = System.currentTimeMillis();
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, Map.class
            );
            long duration = System.currentTimeMillis() - startTime;
            
            // 解析响应
            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new RuntimeException("Rerank API 返回空响应");
            }
            
            // 检查错误
            if (body.containsKey("code") && !"200".equals(body.get("code").toString())) {
                String errorMsg = body.containsKey("message") ? body.get("message").toString() : "未知错误";
                throw new RuntimeException("Rerank API 调用失败: " + errorMsg);
            }
            
            Map<String, Object> output = (Map<String, Object>) body.get("output");
            if (output == null) {
                throw new RuntimeException("Rerank API 返回数据格式错误: 缺少 output 字段");
            }
            
            List<Map<String, Object>> results = (List<Map<String, Object>>) output.get("results");
            if (results == null) {
                throw new RuntimeException("Rerank API 返回数据格式错误: 缺少 results 字段");
            }
            
            // 转换为 RerankResult
            List<RerankResult> rerankResults = new ArrayList<>();
            for (Map<String, Object> result : results) {
                RerankResult rr = new RerankResult();
                rr.setIndex(((Number) result.get("index")).intValue());
                rr.setDocument((String) result.get("document"));
                
                // relevance_score 可能是 Double 或 Integer
                Object scoreObj = result.get("relevance_score");
                double score = scoreObj instanceof Number ? ((Number) scoreObj).doubleValue() : 0.0;
                rr.setRelevanceScore(score);
                
                rerankResults.add(rr);
            }
            
            log.info("✅ Rerank 完成: 输入{}条, 返回{}条, 耗时{}ms", 
                documents.size(), rerankResults.size(), duration);
            
            // 打印前3个结果的分数
            if (!rerankResults.isEmpty()) {
                log.info("📊 Top 3 分数:");
                for (int i = 0; i < Math.min(3, rerankResults.size()); i++) {
                    RerankResult rr = rerankResults.get(i);
                    String preview = rr.getDocument().substring(0, Math.min(50, rr.getDocument().length()));
                    log.info("  [{}] 分数: {} | 预览: {}...", 
                        i + 1, String.format("%.4f", rr.getRelevanceScore()), preview);
                }
            }
            
            return rerankResults;
            
        } catch (Exception e) {
            log.error("❌ Rerank API 调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("Rerank 失败: " + e.getMessage(), e);
        }
    }
}
