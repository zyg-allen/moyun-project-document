package com.moyun.agent.dto;

import com.moyun.agent.entity.ModelConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 模型配置列表响应DTO
 *
 * <p>包含所有模型配置的列表数据</p>
 *
 * @author laomao
 * @time 2025/11/23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelConfigListResponse {

    /**
     * 模型配置列表
     */
    private List<ModelConfig> configs;

    /**
     * 总数量
     */
    private Integer total;
}
