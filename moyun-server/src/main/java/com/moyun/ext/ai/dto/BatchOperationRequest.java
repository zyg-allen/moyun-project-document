package com.moyun.ext.ai.dto;

import lombok.Data;
import java.util.List;

/**
 * 批量操作请求
 *
 * @author laomao
 */
@Data
public class BatchOperationRequest {
    
    /**
     * 操作类型：setCategory(设置分组), addTags(添加标签), delete(删除)
     */
    private String operation;
    
    /**
     * 知识库ID列表
     */
    private List<Long> ids;
    
    /**
     * 分组（用于setCategory操作）
     */
    private String category;
    
    /**
     * 标签（JSON字符串，用于addTags操作）
     */
    private String tags;
}
