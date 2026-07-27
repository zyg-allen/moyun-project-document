package com.moyun.agent.controller;

import com.moyun.agent.common.ApiResponse;
import com.moyun.agent.exception.BusinessException;
import com.moyun.agent.exception.ErrorCode;
import com.moyun.agent.common.ListResponse;
import com.moyun.agent.entity.ModelConfig;
import com.moyun.agent.service.ModelConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 模型配置控制器
 *
 * <p>提供AI模型配置的增删改查功能</p>
 *
 * @author laomao
 * @time 2025/11/23
 */
@Slf4j
@Tag(name = "模型配置管理")
@RestController
@RequestMapping("/api/model-config")
public class ModelConfigController {

    @Autowired
    private ModelConfigService modelConfigService;

    /**
     * 获取所有模型配置
     *
     * <p>查询系统中所有的模型配置，按创建时间倒序排列</p>
     * <p>返回时 API Key 会进行脱敏处理，只显示前4位和后4位</p>
     *
     * @return 模型配置列表
     */
    @Operation(summary = "获取所有模型配置", description = "查询所有模型配置列表")
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<ListResponse<ModelConfig>>> list() {
        try {
            List<ModelConfig> list = modelConfigService.listOrderByCreateTimeDesc();
            // 对 API Key 进行脱敏处理
            list.forEach(config -> config.setApiKey(maskApiKey(config.getApiKey())));
            return ResponseEntity.ok(ApiResponse.success(new ListResponse<>(list)));
        } catch (Exception e) {
            log.error("获取模型配置列表失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取失败: " + e.getMessage()));
        }
    }
    
    /**
     * API Key 脱敏处理
     *
     * <p>只显示前4位和后4位，中间用****替代</p>
     *
     * @param apiKey 原始 API Key
     * @return 脱敏后的 API Key
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return apiKey;
        }
        int length = apiKey.length();
        if (length <= 8) {
            // 太短则全部隐藏
            return "****";
        }
        // 显示前4位和后4位
        return apiKey.substring(0, 4) + "****" + apiKey.substring(length - 4);
    }

    /**
     * 获取单个模型配置
     *
     * <p>根据ID查询指定的模型配置详情</p>
     *
     * @param id 模型配置ID
     * @return 模型配置详情
     */
    @Operation(summary = "获取单个模型配置", description = "根据ID查询模型配置详情")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ModelConfig>> getById(@PathVariable("id") Long id) {
        try {
            ModelConfig config = modelConfigService.getById(id);
            if (config == null) {
                return ResponseEntity.ok(ApiResponse.error("模型配置不存在"));
            }
            // 对 API Key 进行脱敏处理
            config.setApiKey(maskApiKey(config.getApiKey()));
            return ResponseEntity.ok(ApiResponse.success(config));
        } catch (Exception e) {
            log.error("获取模型配置失败 - ID: {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("获取失败: " + e.getMessage()));
        }
    }

    /**
     * 创建模型配置
     *
     * <p>创建新的AI模型配置，如果是第一个配置会自动设为默认</p>
     *
     * @param config 模型配置信息
     * @return 创建的模型配置
     */
    @Operation(summary = "创建模型配置", description = "创建新的模型配置")
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<ModelConfig>> create(@RequestBody ModelConfig config) {
        try {
            // 设置创建和更新时间
            config.setCreateTime(LocalDateTime.now());
            config.setUpdateTime(LocalDateTime.now());

            // 如果是第一个配置，自动设为默认
            if (modelConfigService.count() == 0) {
                config.setIsDefault(true);
                log.info("创建第一个模型配置，自动设为默认");
            }

            modelConfigService.save(config);
            log.info("创建模型配置成功 - ID: {}, 名称: {}", config.getId(), config.getName());
            return ResponseEntity.ok(ApiResponse.success("创建成功", config));
        } catch (Exception e) {
            log.error("创建模型配置失败", e);
            return ResponseEntity.ok(ApiResponse.error("创建失败: " + e.getMessage()));
        }
    }

    /**
     * 更新模型配置
     *
     * <p>更新现有的模型配置信息</p>
     *
     * @param config 模型配置信息
     * @return 更新后的模型配置
     */
    @Operation(summary = "更新模型配置", description = "更新现有模型配置")
    @PutMapping("/update")
    public ResponseEntity<ApiResponse<ModelConfig>> update(@RequestBody ModelConfig config) {
        try {
            config.setUpdateTime(LocalDateTime.now());
            modelConfigService.updateById(config);
            log.info("更新模型配置成功 - ID: {}", config.getId());
            return ResponseEntity.ok(ApiResponse.success("更新成功", config));
        } catch (Exception e) {
            log.error("更新模型配置失败 - ID: {}", config.getId(), e);
            return ResponseEntity.ok(ApiResponse.error("更新失败: " + e.getMessage()));
        }
    }

    /**
     * 删除模型配置
     *
     * <p>删除指定的模型配置，默认模型不能删除</p>
     *
     * @param id 模型配置ID
     * @return 删除结果
     */
    @Operation(summary = "删除模型配置", description = "删除指定的模型配置")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
        try {
            // 检查是否为默认配置
            ModelConfig config = modelConfigService.getById(id);
            if (config != null && config.getIsDefault()) {
                return ResponseEntity.ok(ApiResponse.error("不能删除默认模型配置"));
            }

            modelConfigService.removeById(id);
            log.info("删除模型配置成功 - ID: {}", id);
            return ResponseEntity.ok(ApiResponse.success("删除成功"));
        } catch (Exception e) {
            log.error("删除模型配置失败 - ID: {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("删除失败: " + e.getMessage()));
        }
    }

    /**
     * 设置默认模型
     *
     * <p>将指定的模型配置设为默认模型</p>
     *
     * @param id 模型配置ID
     * @return 设置结果
     */
    @Operation(summary = "设置默认模型", description = "将指定模型设为默认")
    @PostMapping("/set-default/{id}")
    public ResponseEntity<ApiResponse<Void>> setDefault(@PathVariable("id") Long id) {
        try {
            modelConfigService.setDefault(id);
            log.info("设置默认模型成功 - ID: {}", id);
            return ResponseEntity.ok(ApiResponse.success("设置成功"));
        } catch (Exception e) {
            log.error("设置默认模型失败 - ID: {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("设置失败: " + e.getMessage()));
        }
    }

    /**
     * 获取默认模型配置
     *
     * <p>获取指定类型的默认模型配置</p>
     *
     * @param type 模型类型，chat或embedding，默认为chat
     * @return 默认模型配置
     */
    @Operation(summary = "获取默认模型配置", description = "获取指定类型的默认模型")
    @GetMapping("/default")
    public ResponseEntity<ApiResponse<ModelConfig>> getDefault(
            @RequestParam(required = false, defaultValue = "chat") String type) {
        try {
            ModelConfig config;
            if ("embedding".equals(type)) {
                config = modelConfigService.getDefaultEmbeddingConfig();
            } else {
                config = modelConfigService.getDefaultChatConfig();
            }

            if (config == null) {
                return ResponseEntity.ok(ApiResponse.error("未找到默认" + type + "模型配置"));
            }

            // 对 API Key 进行脱敏处理
            config.setApiKey(maskApiKey(config.getApiKey()));
            return ResponseEntity.ok(ApiResponse.success(config));
        } catch (Exception e) {
            log.error("获取默认模型配置失败 - type: {}", type, e);
            return ResponseEntity.ok(ApiResponse.error("获取失败: " + e.getMessage()));
        }
    }

    /**
     * 测试模型连接
     *
     * <p>测试与AI模型的连接是否正常，实际调用模型API进行验证</p>
     *
     * @param config 模型配置信息
     * @return 测试结果
     */
    @Operation(summary = "测试模型连接", description = "测试与AI模型的连接")
    @PostMapping("/test")
    public ResponseEntity<ApiResponse<String>> testConnection(@RequestBody ModelConfig config) {
        log.info("开始测试模型连接 - 名称: {}, 类型: {}, 提供商: {}",
            config.getName(), config.getModelType(), config.getProvider());

        try {
            // 参数验证
            if (config.getProvider() == null || config.getProvider().isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("请选择模型提供商"));
            }

            if (config.getModelName() == null || config.getModelName().isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("请输入模型名称"));
            }

            // 根据不同提供商进行测试
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
                    return ResponseEntity.ok(ApiResponse.error("不支持的提供商: " + provider));
            }

            log.info("测试成功 - {}", testResult);
            return ResponseEntity.ok(ApiResponse.success("连接测试成功！" + testResult, testResult));

        } catch (Exception e) {
            log.error("测试模型连接失败 - 名称: {}, 错误: {}", config.getName(), e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("连接测试失败: " + e.getMessage()));
        }
    }

    /**
     * 测试OpenAI连接
     */
    private String testOpenAI(ModelConfig config) {
        // 验证必需参数
        if (config.getApiKey() == null || config.getApiKey().isEmpty()) {
            throw new BusinessException(ErrorCode.API_KEY_INVALID, "OpenAI需要配置API Key");
        }

        // 验证模型类型
        if (config.getModelType() == null || config.getModelType().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择模型类型（chat/embedding）");
        }

        try {
            // 使用配置的baseUrl，默认OpenAI官方地址
            String baseUrl = config.getBaseUrl() != null && !config.getBaseUrl().isEmpty()
                ? config.getBaseUrl() : "https://api.openai.com/v1";

            // 使用配置的超时时间，默认30秒
            int timeoutSeconds = config.getTimeout() != null && config.getTimeout() > 0
                ? config.getTimeout() : 30;

            log.info("测试OpenAI - URL: {}, 模型: {}, 类型: {}, 超时: {}s",
                baseUrl, config.getModelName(), config.getModelType(), timeoutSeconds);

            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();

            // 根据模型类型构造请求
            String requestBody;
            String endpoint;

            if ("chat".equals(config.getModelType())) {
                // 对话模型测试（包括多模态模型，也支持纯文本对话）
                int maxTokens = config.getMaxTokens() != null && config.getMaxTokens() > 0
                    ? Math.min(config.getMaxTokens(), 10) : 10; // 测试时最多10个token
                double temperature = config.getTemperature() != null
                    ? config.getTemperature() : 0.7;

                requestBody = String.format(
                    "{\"model\":\"%s\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}],\"max_tokens\":%d,\"temperature\":%.1f}",
                    config.getModelName(), maxTokens, temperature
                );
                endpoint = "/chat/completions";
            } else if ("embedding".equals(config.getModelType())) {
                // 向量模型测试
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

    /**
     * 测试通义千问连接
     */
    private String testDashScope(ModelConfig config) {
        if (config.getApiKey() == null || config.getApiKey().isEmpty()) {
            throw new BusinessException(ErrorCode.API_KEY_INVALID, "通义千问需要配置API Key");
        }

        if (config.getModelType() == null || config.getModelType().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择模型类型（chat/embedding）");
        }

        try {
            // 使用配置的baseUrl或默认地址
            String baseUrl = config.getBaseUrl() != null && !config.getBaseUrl().isEmpty()
                ? config.getBaseUrl() : "https://dashscope.aliyuncs.com/api/v1";

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
                // 对话模型测试（包括多模态模型）
                int maxTokens = config.getMaxTokens() != null && config.getMaxTokens() > 0
                    ? Math.min(config.getMaxTokens(), 10) : 10;
                
                // VL 模型（通过模型名判断）使用 OpenAI 兼容模式
                boolean isVlModel = config.getModelName() != null && config.getModelName().toLowerCase().contains("-vl");
                if (isVlModel) {
                    baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
                    requestBody = String.format(
                        "{\"model\":\"%s\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}],\"max_tokens\":%d}",
                        config.getModelName(), maxTokens
                    );
                    endpoint = "/chat/completions";
                } else {
                    requestBody = String.format(
                        "{\"model\":\"%s\",\"input\":{\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}]},\"parameters\":{\"max_tokens\":%d}}",
                        config.getModelName(), maxTokens
                    );
                    endpoint = "/services/aigc/text-generation/generation";
                }
            } else if ("embedding".equals(config.getModelType())) {
                requestBody = String.format(
                    "{\"model\":\"%s\",\"input\":{\"texts\":[\"test embedding\"]}}",
                    config.getModelName()
                );
                endpoint = "/services/embeddings/text-embedding/text-embedding";
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

    /**
     * 测试Ollama连接
     */
    private String testOllama(ModelConfig config) {
        if (config.getModelType() == null || config.getModelType().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择模型类型（chat/embedding）");
        }

        try {
            // 使用配置的baseUrl
            String baseUrl = config.getBaseUrl() != null && !config.getBaseUrl().isEmpty()
                ? config.getBaseUrl() : "http://localhost:11434";

            int timeoutSeconds = config.getTimeout() != null && config.getTimeout() > 0
                ? config.getTimeout() : 60; // Ollama默认60秒超时

            log.info("测试Ollama - URL: {}, 模型: {}, 类型: {}",
                baseUrl, config.getModelName(), config.getModelType());

            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();

            // 先测试Ollama服务是否运行
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

            // 根据模型类型测试
            String requestBody;
            String endpoint;

            if ("chat".equals(config.getModelType())) {
                // 对话模型测试（包括多模态模型）
                requestBody = String.format(
                    "{\"model\":\"%s\",\"prompt\":\"hi\",\"stream\":false}",
                    config.getModelName()
                );
                endpoint = "/api/generate";
            } else if ("embedding".equals(config.getModelType())) {
                // 向量模型测试
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
