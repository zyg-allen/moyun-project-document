package com.moyun.agent.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 本地部署的多模态 Embedding 模型实现
 * 
 * <p>通过 vLLM 服务调用本地部署的 Qwen3-VL-Embedding 模型。
 * 需要先部署 vLLM 服务：</p>
 * 
 * <pre>
 * vllm serve Qwen/Qwen3-VL-Embedding-8B \
 *     --host 0.0.0.0 \
 *     --port 8000 \
 *     --dtype bfloat16 \
 *     --max-model-len 32768
 * </pre>
 * 
 * <p><b>注意：</b>此实现为示例代码，需要根据实际的 vLLM API 格式调整。
 * 等待 DashScope API 支持后，可以创建 DashScopeMultimodalEmbeddingModel 替代。</p>
 * 
 * @author laomao
 * @since 2025-01-22
 */
@Slf4j
public class LocalMultimodalEmbeddingModel implements MultimodalEmbeddingModel {
    
    private final String baseUrl;
    private final int dimension;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    /**
     * 构造函数
     * 
     * @param baseUrl vLLM 服务地址（如 http://localhost:8000）
     * @param dimension 向量维度（2048 或 4096）
     */
    public LocalMultimodalEmbeddingModel(String baseUrl, int dimension) {
        this.baseUrl = baseUrl;
        this.dimension = dimension;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public float[] embedText(String text) {
        log.info("🔤 向量化文本: {}", text.substring(0, Math.min(50, text.length())));
        
        MultimodalInput input = MultimodalInput.text(text);
        List<float[]> results = embedBatch(Collections.singletonList(input));
        
        return results.isEmpty() ? new float[dimension] : results.get(0);
    }
    
    @Override
    public float[] embedImage(byte[] imageBytes) {
        log.info("🖼️ 向量化图片: {} bytes", imageBytes.length);
        
        MultimodalInput input = MultimodalInput.image(imageBytes);
        List<float[]> results = embedBatch(Collections.singletonList(input));
        
        return results.isEmpty() ? new float[dimension] : results.get(0);
    }
    
    @Override
    public float[] embedImage(String imageUrl) {
        log.info("🖼️ 向量化图片 URL: {}", imageUrl);
        
        MultimodalInput input = MultimodalInput.image(imageUrl);
        List<float[]> results = embedBatch(Collections.singletonList(input));
        
        return results.isEmpty() ? new float[dimension] : results.get(0);
    }
    
    @Override
    public float[] embedVideo(byte[] videoBytes, double fps, int maxFrames) {
        log.info("🎬 向量化视频: {} bytes, fps={}, maxFrames={}", 
            videoBytes.length, fps, maxFrames);
        
        MultimodalInput input = MultimodalInput.video(videoBytes, fps, maxFrames);
        List<float[]> results = embedBatch(Collections.singletonList(input));
        
        return results.isEmpty() ? new float[dimension] : results.get(0);
    }
    
    @Override
    public float[] embedMultimodal(String text, byte[] imageBytes) {
        log.info("🔀 向量化多模态: 文本({} chars) + 图片({} bytes)", 
            text.length(), imageBytes.length);
        
        MultimodalInput input = MultimodalInput.textImage(text, imageBytes);
        List<float[]> results = embedBatch(Collections.singletonList(input));
        
        return results.isEmpty() ? new float[dimension] : results.get(0);
    }
    
    @Override
    public List<float[]> embedBatch(List<MultimodalInput> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            log.warn("⚠️ 输入为空");
            return new ArrayList<>();
        }
        
        log.info("📦 批量向量化: {} 个输入", inputs.size());
        
        try {
            // 构建请求体（根据实际 vLLM API 格式调整）
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "Qwen3-VL-Embedding-8B");
            requestBody.put("inputs", convertInputs(inputs));
            requestBody.put("dimension", dimension);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            // 发送请求
            String url = baseUrl + "/v1/embeddings";
            long startTime = System.currentTimeMillis();
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, Map.class
            );
            
            long duration = System.currentTimeMillis() - startTime;
            
            // 解析响应
            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new RuntimeException("vLLM 返回空响应");
            }
            
            List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");
            List<float[]> embeddings = new ArrayList<>();
            
            for (Map<String, Object> item : data) {
                List<Double> embedding = (List<Double>) item.get("embedding");
                float[] vector = new float[embedding.size()];
                for (int i = 0; i < embedding.size(); i++) {
                    vector[i] = embedding.get(i).floatValue();
                }
                embeddings.add(vector);
            }
            
            log.info("✅ 批量向量化完成: {} 个向量, 维度={}, 耗时={}ms", 
                embeddings.size(), dimension, duration);
            
            return embeddings;
            
        } catch (Exception e) {
            log.error("❌ 向量化失败: {}", e.getMessage(), e);
            throw new RuntimeException("向量化失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public int getDimension() {
        return dimension;
    }
    
    @Override
    public boolean supportsMRL() {
        return true; // Qwen3-VL-Embedding 支持 MRL
    }
    
    @Override
    public void setDimension(int dimension) {
        // 注意：这里只是示例，实际需要重新初始化模型
        log.warn("⚠️ 动态修改维度需要重新初始化模型");
    }
    
    /**
     * 转换输入格式（根据实际 vLLM API 格式调整）
     */
    private List<Map<String, Object>> convertInputs(List<MultimodalInput> inputs) {
        List<Map<String, Object>> converted = new ArrayList<>();
        
        for (MultimodalInput input : inputs) {
            Map<String, Object> item = new HashMap<>();
            
            if (input.getText() != null) {
                item.put("text", input.getText());
            }
            
            if (input.getImageBytes() != null) {
                // 转换为 Base64
                String base64 = Base64.getEncoder().encodeToString(input.getImageBytes());
                item.put("image", base64);
            } else if (input.getImageUrl() != null) {
                item.put("image", input.getImageUrl());
            }
            
            if (input.getVideoBytes() != null) {
                String base64 = Base64.getEncoder().encodeToString(input.getVideoBytes());
                item.put("video", base64);
                item.put("fps", input.getFps() != null ? input.getFps() : 1.0);
                item.put("max_frames", input.getMaxFrames() != null ? input.getMaxFrames() : 64);
            } else if (input.getVideoUrl() != null) {
                item.put("video", input.getVideoUrl());
                item.put("fps", input.getFps() != null ? input.getFps() : 1.0);
                item.put("max_frames", input.getMaxFrames() != null ? input.getMaxFrames() : 64);
            }
            
            if (input.getInstruction() != null) {
                item.put("instruction", input.getInstruction());
            }
            
            converted.add(item);
        }
        
        return converted;
    }
}
