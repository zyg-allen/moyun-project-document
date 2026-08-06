package com.moyun.ext.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模型配置实体类
 *
 * <p>对应数据库表 model_config，存储AI模型配置（Chat、Embedding、多模态）</p>
 *
 * @author laomao
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("model_config")
public class ModelConfig {
    @TableId(type = IdType.AUTO)
    private Long id;

    // 配置名称（用于显示）
    private String name;

    // 模型提供商：openai, ollama, dashscope 等
    private String provider;

    // 模型类型：chat（对话）, embedding（向量）
    private String modelType;

    // 模型名称
    private String modelName;

    // API Key（OpenAI、通义千问等需要）
    private String apiKey;

    // Base URL（自定义 API 地址）
    private String baseUrl;

    // 温度参数（0-2）
    private Double temperature;

    // 最大 Token 数
    private Integer maxTokens;

    // 超时时间（秒）
    private Integer timeout;

    // 是否支持流式输出
    private Boolean streamingSupported;

    // 是否启用
    private Boolean enabled;

    // 是否为默认模型
    private Boolean isDefault;

    // 备注说明
    private String description;

    // 输入价格（元/1000 tokens）
    private BigDecimal inputPrice;

    // 输出价格（元/1000 tokens）
    private BigDecimal outputPrice;

    // 创建时间
    private LocalDateTime createTime;

    // 更新时间
    private LocalDateTime updateTime;
}
