package com.moyun.ext.ai.dto;

import lombok.Data;

/**
 * 架构图生成请求 DTO
 *
 * @author laomao
 */
@Data
public class DiagramGenerateDTO {
    
    /**
     * 架构描述内容
     */
    private String content;
    
    /**
     * 图表风格: normal(普通) / enterprise(企业级)
     */
    private String style;
}
