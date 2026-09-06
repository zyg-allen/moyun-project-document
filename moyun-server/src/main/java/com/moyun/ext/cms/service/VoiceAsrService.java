package com.moyun.ext.cms.service;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.ai.entity.ModelConfig;
import com.moyun.ext.ai.service.AiProviderService;
import com.moyun.ext.ai.service.ModelConfigService;

/**
 * 语音转文字服务（Qwen-ASR，OpenAI 兼容模式）—— V10.1 语音面试官
 *
 * <p>浏览器 Web Speech API 在国内网络环境（依赖 Google 服务）大多不可用，
 * 前端在不可用/识别失败时降级为 MediaRecorder 录音，上传本服务转写。</p>
 *
 * <p>调用 Qwen3-ASR-Flash（OpenAI 兼容协议）：
 * POST {baseUrl}/chat/completions
 * body: {"model":"qwen3-asr-flash","messages":[{"role":"user","content":[
 *   {"type":"input_audio","input_audio":{"data":"data:audio/wav;base64,..."}}]}]}
 * 响应: choices[0].message.content 即转写文本。</p>
 *
 * <p>API 文档: https://help.aliyun.com/zh/model-studio/qwen-asr-api-reference</p>
 *
 * <p>配置取值优先级：后台「AI 模块 → 模型配置」中 model_type='asr' 的默认启用配置
 * （api_key/base_url/model_name，密钥库中加密存储）→ yaml 兜底
 * （moyun.ai.api-key / asr-base-url / asr-model）。</p>
 *
 * @author moyun
 */
@Service
public class VoiceAsrService {

    private static final Logger log = LoggerFactory.getLogger(VoiceAsrService.class);

    /** 模型配置服务（后台「AI 模块 → 模型配置」） */
    @Autowired(required = false)
    private ModelConfigService modelConfigService;

    /** 提供商注册表（baseUrl 留空时按 provider 兜底，与 chat 工厂 resolveBaseUrl 行为一致） */
    @Autowired(required = false)
    private AiProviderService aiProviderService;

    /** moyun.ai.api-key（yaml 兜底密钥） */
    @Value("${moyun.ai.api-key:}")
    private String moyunAiApiKey;

    /** langchain4j.dashscope.api-key（DashScope 专用密钥，兜底） */
    @Value("${langchain4j.dashscope.api-key:}")
    private String dashscopeApiKey;

    /** ASR OpenAI 兼容地址前缀（yaml 兜底，默认公共 DashScope） */
    @Value("${moyun.ai.asr-base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String asrBaseUrl;

    /** ASR 模型（yaml 兜底，默认 qwen3-asr-flash） */
    @Value("${moyun.ai.asr-model:qwen3-asr-flash}")
    private String asrModel;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RestTemplate restTemplate;

    public VoiceAsrService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(60_000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 转写音频文件为文本
     *
     * @param audio 前端上传的录音（WAV 16kHz 单声道，由前端 MediaRecorder 录制并转码）
     * @return 转写文本（无有效语音时返回空串）
     * @throws IllegalStateException 未配置 API Key
     * @throws RuntimeException     ASR 调用失败
     */
    public String transcribe(MultipartFile audio) {
        AsrEndpoint endpoint = resolveEndpoint();
        if (endpoint.apiKey() == null || endpoint.apiKey().isBlank()) {
            throw new IllegalStateException("语音识别服务未配置：请在后台「AI 模块 → 模型配置」添加 model_type='asr' 的默认启用配置，或设置 moyun.ai.api-key");
        }
        try {
            byte[] bytes = audio.getBytes();
            if (bytes.length == 0) {
                return "";
            }

            // Base64 Data URL 音频输入（Qwen3-ASR-Flash OpenAI 兼容协议）
            String dataUrl = "data:audio/wav;base64," + Base64.getEncoder().encodeToString(bytes);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(endpoint.apiKey());
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "model", endpoint.model(),
                    "messages", List.of(Map.of(
                            "role", "user",
                            "content", List.of(Map.of(
                                    "type", "input_audio",
                                    "input_audio", Map.of("data", dataUrl)
                            ))
                    ))
            );

            ResponseEntity<String> response =
                    restTemplate.postForEntity(endpoint.chatUrl(), new HttpEntity<>(body, headers), String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            String text = root.path("choices").path(0).path("message").path("content").asText("");
            if (text.isBlank()) {
                log.info("[ASR] 转写结果为空（音频可能无人声），audioBytes={}", bytes.length);
            }
            return text.trim();
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("[ASR] 语音识别调用失败", e);
            throw new RuntimeException("语音识别失败：" + e.getMessage(), e);
        }
    }

    /** 解析后的 ASR 调用参数（apiKey + model + chatUrl） */
    private record AsrEndpoint(String apiKey, String model, String chatUrl) {
    }

    /**
     * 解析 ASR 调用参数：后台模型配置（model_type='asr' 默认启用项）优先，
     * 字段级回退 yaml（moyun.ai.asr-model / asr-base-url / api-key）。
     */
    private AsrEndpoint resolveEndpoint() {
        String apiKey = null;
        String baseUrl = null;
        String model = null;

        // 1. 后台模型配置（api_key 已透明解密）
        if (modelConfigService != null) {
            try {
                ModelConfig config = modelConfigService.getDefaultAsrConfig();
                if (config != null) {
                    apiKey = config.getApiKey();
                    baseUrl = config.getBaseUrl();
                    model = config.getModelName();
                    // baseUrl 留空时按提供商注册表默认地址兜底（与 chat 工厂 resolveBaseUrl 一致）
                    if ((baseUrl == null || baseUrl.isBlank()) && aiProviderService != null) {
                        String def = aiProviderService.defaultBaseUrl(config.getProvider());
                        if (def != null && !def.isBlank()) {
                            baseUrl = def;
                        }
                    }
                    log.debug("[ASR] 使用后台模型配置: id={}, model={}", config.getId(), model);
                }
            } catch (Exception e) {
                log.warn("[ASR] 读取后台模型配置失败，回退 yaml 配置: {}", e.getMessage());
            }
        }

        // 2. 字段级回退 yaml
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = (moyunAiApiKey != null && !moyunAiApiKey.isBlank())
                    ? moyunAiApiKey : dashscopeApiKey;
        }
        if (model == null || model.isBlank()) {
            model = asrModel;
        }

        // 3. 拼接 chat/completions 地址（容忍末尾多余 /）
        String base = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl : asrBaseUrl;
        base = base.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return new AsrEndpoint(apiKey, model, base + "/chat/completions");
    }
}
