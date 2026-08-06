package com.moyun.ext.ai.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ext.ai.common.ListResponse;
import com.moyun.ext.ai.entity.ModelConfig;
import com.moyun.ext.ai.exception.BusinessException;
import com.moyun.ext.ai.exception.ErrorCode;
import com.moyun.ext.ai.service.ModelConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Tag(name = "模型配置管理")
@RestController
@RequestMapping("/cms/ai/model-config")
public class ModelConfigController {

    @Autowired
    private ModelConfigService modelConfigService;

    @Operation(summary = "获取所有模型配置", description = "查询所有模型配置列表")
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('cms:ai:model-config:list')")
    public AjaxResult list() {
        try {
            List<ModelConfig> list = modelConfigService.listOrderByCreateTimeDesc();
            // listOrderByCreateTimeDesc 走 super.list() 返回 DB 原始密文（ENC: 格式），
            // 这里先解密再掩码，避免掩码字符串与 ENC: 密文拼接产生错误展示
            list.forEach(config -> config.setApiKey(maskApiKey(decryptIfNeeded(config.getApiKey()))));
            return AjaxResult.success(new ListResponse<>(list));
        } catch (Exception e) {
            log.error("获取模型配置列表失败", e);
            return AjaxResult.error("获取失败: " + e.getMessage());
        }
    }

    /**
     * 若 apiKey 是 ENC: 密文则解密为明文，否则原样返回（兼容历史明文与前端传入值）。
     * 仅用于展示前预处理，不会修改 DB。
     */
    private String decryptIfNeeded(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return apiKey;
        }
        if (com.moyun.ext.ai.util.ApiKeyCryptoUtils.isEncrypted(apiKey)) {
            return com.moyun.ext.ai.util.ApiKeyCryptoUtils.decrypt(apiKey);
        }
        return apiKey;
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return apiKey;
        }
        int length = apiKey.length();
        if (length <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(length - 4);
    }

    @Operation(summary = "获取单个模型配置", description = "根据ID查询模型配置详情")
    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('cms:ai:model-config:query')")
    public AjaxResult getById(@PathVariable("id") Long id) {
        try {
            ModelConfig config = modelConfigService.getById(id);
            if (config == null) {
                return AjaxResult.error("模型配置不存在");
            }
            config.setApiKey(maskApiKey(config.getApiKey()));
            return AjaxResult.success(config);
        } catch (Exception e) {
            log.error("获取模型配置失败 - ID: {}", id, e);
            return AjaxResult.error("获取失败: " + e.getMessage());
        }
    }

    @Operation(summary = "创建模型配置", description = "创建新的模型配置")
    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermi('cms:ai:model-config:add')")
    public AjaxResult create(@RequestBody ModelConfig config) {
        try {
            config.setCreateTime(LocalDateTime.now());
            config.setUpdateTime(LocalDateTime.now());

            if (modelConfigService.count() == 0) {
                config.setIsDefault(true);
                log.info("创建第一个模型配置，自动设为默认");
            }

            modelConfigService.save(config);
            log.info("创建模型配置成功 - ID: {}, 名称: {}", config.getId(), config.getName());
            return AjaxResult.success("创建成功", config);
        } catch (Exception e) {
            log.error("创建模型配置失败", e);
            return AjaxResult.error("创建失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新模型配置", description = "更新现有模型配置")
    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermi('cms:ai:model-config:edit')")
    public AjaxResult update(@RequestBody ModelConfig config) {
        try {
            config.setUpdateTime(LocalDateTime.now());
            modelConfigService.updateById(config);
            log.info("更新模型配置成功 - ID: {}", config.getId());
            return AjaxResult.success("更新成功", config);
        } catch (Exception e) {
            log.error("更新模型配置失败 - ID: {}", config.getId(), e);
            return AjaxResult.error("更新失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除模型配置", description = "删除指定的模型配置")
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('cms:ai:model-config:remove')")
    public AjaxResult delete(@PathVariable("id") Long id) {
        try {
            ModelConfig config = modelConfigService.getById(id);
            if (config != null && config.getIsDefault()) {
                return AjaxResult.error("不能删除默认模型配置");
            }

            modelConfigService.removeById(id);
            log.info("删除模型配置成功 - ID: {}", id);
            return AjaxResult.success("删除成功");
        } catch (Exception e) {
            log.error("删除模型配置失败 - ID: {}", id, e);
            return AjaxResult.error("删除失败: " + e.getMessage());
        }
    }

    @Operation(summary = "设置默认模型", description = "将指定模型设为默认")
    @PostMapping("/set-default/{id}")
    @PreAuthorize("@ss.hasPermi('cms:ai:model-config:edit')")
    public AjaxResult setDefault(@PathVariable("id") Long id) {
        try {
            modelConfigService.setDefault(id);
            log.info("设置默认模型成功 - ID: {}", id);
            return AjaxResult.success("设置成功");
        } catch (Exception e) {
            log.error("设置默认模型失败 - ID: {}", id, e);
            return AjaxResult.error("设置失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取默认模型配置", description = "获取指定类型的默认模型")
    @GetMapping("/default")
    @PreAuthorize("@ss.hasPermi('cms:ai:model-config:query')")
    public AjaxResult getDefault(
            @RequestParam(required = false, defaultValue = "chat") String type) {
        try {
            ModelConfig config;
            if ("embedding".equals(type)) {
                config = modelConfigService.getDefaultEmbeddingConfig();
            } else {
                config = modelConfigService.getDefaultChatConfig();
            }

            if (config == null) {
                return AjaxResult.error("未找到默认" + type + "模型配置");
            }

            config.setApiKey(maskApiKey(config.getApiKey()));
            return AjaxResult.success(config);
        } catch (Exception e) {
            log.error("获取默认模型配置失败 - type: {}", type, e);
            return AjaxResult.error("获取失败: " + e.getMessage());
        }
    }

    @Operation(summary = "测试模型连接", description = "测试与AI模型的连接")
    @PostMapping("/test")
    @PreAuthorize("@ss.hasPermi('cms:ai:model-config:query')")
    public AjaxResult testConnection(@RequestBody ModelConfig config) {
        log.info("开始测试模型连接 - 名称: {}, 类型: {}, 提供商: {}",
            config.getName(), config.getModelType(), config.getProvider());

        try {
            if (config.getProvider() == null || config.getProvider().isEmpty()) {
                return AjaxResult.error("请选择模型提供商");
            }

            if (config.getModelName() == null || config.getModelName().isEmpty()) {
                return AjaxResult.error("请输入模型名称");
            }

            // 处理 apiKey 掩码：前端若回显掩码（含 **** 且非 ENC: 前缀），从 DB 读取真实 apiKey
            String apiKey = config.getApiKey();
            if (apiKey != null && apiKey.contains("****")
                    && !com.moyun.ext.ai.util.ApiKeyCryptoUtils.isEncrypted(apiKey)
                    && config.getId() != null) {
                // 从 DB 读取（getById 会自动解密）
                ModelConfig dbConfig = modelConfigService.getById(config.getId());
                if (dbConfig != null && dbConfig.getApiKey() != null) {
                    apiKey = dbConfig.getApiKey();
                    config.setApiKey(apiKey);
                } else {
                    return AjaxResult.error("未找到原 apiKey，请重新输入完整的 API Key");
                }
            }

            String provider = config.getProvider().toLowerCase();
            String testResult;

            switch (provider) {
                case "openai":
                    testResult = testOpenAI(config);
                    break;
                case "dashscope":
                    testResult = testDashScope(config);
                    break;
                case "ollama":
                    testResult = testOllama(config);
                    break;
                default:
                    return AjaxResult.error("不支持的提供商: " + provider);
            }

            log.info("测试成功 - {}", testResult);
            return AjaxResult.success("连接测试成功！" + testResult, testResult);

        } catch (Exception e) {
            log.error("测试模型连接失败 - 名称: {}, 错误: {}", config.getName(), e.getMessage(), e);
            return AjaxResult.error("连接测试失败: " + e.getMessage());
        }
    }

    private String testOpenAI(ModelConfig config) {
        if (config.getApiKey() == null || config.getApiKey().isEmpty()) {
            throw new BusinessException(ErrorCode.API_KEY_INVALID, "OpenAI需要配置API Key");
        }

        if (config.getModelType() == null || config.getModelType().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择模型类型（chat/embedding）");
        }

        try {
            String baseUrl = config.getBaseUrl() != null && !config.getBaseUrl().isEmpty()
                ? config.getBaseUrl() : "https://api.openai.com/v1";

            int timeoutSeconds = config.getTimeout() != null && config.getTimeout() > 0
                ? config.getTimeout() : 30;

            log.info("测试OpenAI - URL: {}, 模型: {}, 类型: {}, 超时: {}s",
                baseUrl, config.getModelName(), config.getModelType(), timeoutSeconds);

            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();

            String requestBody;
            String endpoint;

            if ("chat".equals(config.getModelType())) {
                int maxTokens = config.getMaxTokens() != null && config.getMaxTokens() > 0
                    ? Math.min(config.getMaxTokens(), 10) : 10;
                double temperature = config.getTemperature() != null
                    ? config.getTemperature() : 0.7;

                requestBody = String.format(
                    "{\"model\":\"%s\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}],\"max_tokens\":%d,\"temperature\":%.1f}",
                    config.getModelName(), maxTokens, temperature
                );
                endpoint = "/chat/completions";
            } else if ("embedding".equals(config.getModelType())) {
                requestBody = String.format(
                    "{\"model\":\"%s\",\"input\":\"test embedding\"}",
                    config.getModelName()
                );
                endpoint = "/embeddings";
            } else {
                throw new BusinessException(ErrorCode.MODEL_CONFIG_INVALID, "不支持的模型类型: " + config.getModelType());
            }

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(baseUrl + endpoint))
                .header("Authorization", "Bearer " + config.getApiKey())
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
                .build();

            java.net.http.HttpResponse<String> response = client.send(request,
                java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return String.format(" 模型: %s (类型: %s)",
                    config.getModelName(), config.getModelType());
            } else if (response.statusCode() == 401) {
                throw new BusinessException(ErrorCode.API_KEY_INVALID, "API Key无效或已过期");
            } else if (response.statusCode() == 404) {
                throw new BusinessException(ErrorCode.MODEL_NOT_FOUND, "模型不存在: " + config.getModelName());
            } else {
                throw new BusinessException(ErrorCode.MODEL_CREATE_FAILED, "HTTP " + response.statusCode() + ": " + response.body());
            }
        } catch (java.net.http.HttpTimeoutException e) {
            throw new BusinessException(ErrorCode.MODEL_CREATE_FAILED, "请求超时，请检查网络或增加超时时间");
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.MODEL_CREATE_FAILED, "OpenAI连接失败: " + e.getMessage());
        }
    }

    private String testDashScope(ModelConfig config) {
        if (config.getApiKey() == null || config.getApiKey().isEmpty()) {
            throw new BusinessException(ErrorCode.API_KEY_INVALID, "通义千问需要配置API Key");
        }

        if (config.getModelType() == null || config.getModelType().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择模型类型（chat/embedding）");
        }

        try {
            // 统一使用 OpenAI 兼容模式（与运行时 createDashscopeChatModel 一致）
            String baseUrl = config.getBaseUrl() != null && !config.getBaseUrl().isEmpty()
                ? config.getBaseUrl() : "https://dashscope.aliyuncs.com/compatible-mode/v1";

            int timeoutSeconds = config.getTimeout() != null && config.getTimeout() > 0
                ? config.getTimeout() : 30;

            log.info("测试通义千问 - URL: {}, 模型: {}, 类型: {}",
                baseUrl, config.getModelName(), config.getModelType());

            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();

            String requestBody;
            String endpoint;

            if ("chat".equals(config.getModelType())) {
                int maxTokens = config.getMaxTokens() != null && config.getMaxTokens() > 0
                    ? Math.min(config.getMaxTokens(), 10) : 10;
                // 统一使用 OpenAI 兼容模式（与运行时一致），避免"测试通过但运行失败"
                requestBody = String.format(
                    "{\"model\":\"%s\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}],\"max_tokens\":%d}",
                    config.getModelName(), maxTokens
                );
                endpoint = "/chat/completions";
            } else if ("embedding".equals(config.getModelType())) {
                // Embedding 同样走 OpenAI 兼容模式（/embeddings）
                requestBody = String.format(
                    "{\"model\":\"%s\",\"input\":[\"test embedding\"]}",
                    config.getModelName()
                );
                endpoint = "/embeddings";
            } else {
                throw new BusinessException(ErrorCode.MODEL_CONFIG_INVALID, "不支持的模型类型: " + config.getModelType());
            }

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(baseUrl + endpoint))
                .header("Authorization", "Bearer " + config.getApiKey())
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
                .build();

            java.net.http.HttpResponse<String> response = client.send(request,
                java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return String.format(" 模型: %s (类型: %s)",
                    config.getModelName(), config.getModelType());
            } else if (response.statusCode() == 401) {
                throw new BusinessException(ErrorCode.API_KEY_INVALID);
            } else if (response.statusCode() == 400) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "请求参数错误，请检查模型名称");
            } else {
                throw new BusinessException(ErrorCode.MODEL_CREATE_FAILED, "HTTP " + response.statusCode() + ": " + response.body());
            }
        } catch (java.net.http.HttpTimeoutException e) {
            throw new BusinessException(ErrorCode.MODEL_CREATE_FAILED, "请求超时");
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.MODEL_CREATE_FAILED, "通义千问连接失败: " + e.getMessage());
        }
    }

    private String testOllama(ModelConfig config) {
        if (config.getModelType() == null || config.getModelType().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择模型类型（chat/embedding）");
        }

        try {
            String baseUrl = config.getBaseUrl() != null && !config.getBaseUrl().isEmpty()
                ? config.getBaseUrl() : "http://localhost:11434";

            int timeoutSeconds = config.getTimeout() != null && config.getTimeout() > 0
                ? config.getTimeout() : 60;

            log.info("测试Ollama - URL: {}, 模型: {}, 类型: {}",
                baseUrl, config.getModelName(), config.getModelType());

            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();

            java.net.http.HttpRequest pingRequest = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(baseUrl + "/api/tags"))
                .GET()
                .timeout(java.time.Duration.ofSeconds(10))
                .build();

            java.net.http.HttpResponse<String> pingResponse = client.send(pingRequest,
                java.net.http.HttpResponse.BodyHandlers.ofString());

            if (pingResponse.statusCode() != 200) {
                throw new BusinessException(ErrorCode.DATASOURCE_CONNECT_FAILED, "Ollama服务未运行或无法访问: " + baseUrl);
            }

            String requestBody;
            String endpoint;

            if ("chat".equals(config.getModelType())) {
                requestBody = String.format(
                    "{\"model\":\"%s\",\"prompt\":\"hi\",\"stream\":false}",
                    config.getModelName()
                );
                endpoint = "/api/generate";
            } else if ("embedding".equals(config.getModelType())) {
                requestBody = String.format(
                    "{\"model\":\"%s\",\"prompt\":\"test embedding\"}",
                    config.getModelName()
                );
                endpoint = "/api/embeddings";
            } else {
                throw new BusinessException(ErrorCode.MODEL_CONFIG_INVALID, "不支持的模型类型: " + config.getModelType());
            }

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(baseUrl + endpoint))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
                .build();

            java.net.http.HttpResponse<String> response = client.send(request,
                java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return String.format(" 模型: %s (类型: %s, Ollama服务正常)",
                    config.getModelName(), config.getModelType());
            } else if (response.statusCode() == 404) {
                throw new BusinessException(ErrorCode.MODEL_NOT_FOUND, "模型不存在，请先使用 ollama pull " + config.getModelName() + " 下载");
            } else {
                throw new BusinessException(ErrorCode.MODEL_CREATE_FAILED, "模型调用失败: HTTP " + response.statusCode());
            }
        } catch (java.net.http.HttpTimeoutException e) {
            throw new BusinessException(ErrorCode.MODEL_CREATE_FAILED, "请求超时，模型可能过大或服务响应慢");
        } catch (java.net.ConnectException e) {
            throw new BusinessException(ErrorCode.DATASOURCE_CONNECT_FAILED, "无法连接到Ollama服务，请确认服务已启动");
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.MODEL_CREATE_FAILED, "Ollama连接失败: " + e.getMessage());
        }
    }
}
