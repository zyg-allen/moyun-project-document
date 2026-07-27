package com.moyun.agent.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库视图对象
 *
 * @author laomao
 */
@Data
public class KnowledgeLibraryVO {
    
    private Long id;
    
    /**
     * 知识库名称
     */
    private String name;
    
    /**
     * 知识库描述
     */
    private String description;
    
    /**
     * 知识库图标
     */
    private String icon;
    
    // ========== 统计信息 ==========
    
    /**
     * 文档数量
     */
    private Integer documentCount;
    
    /**
     * 总分段数
     */
    private Integer totalSegments;
    
    /**
     * 总文件大小（字节）
     */
    private Long totalSize;
    
    /**
     * 总文件大小（格式化）
     */
    private String totalSizeFormatted;
    
    /**
     * 使用次数
     */
    private Integer usageCount;
    
    /**
     * 命中次数
     */
    private Integer hitCount;
    
    /**
     * 最后使用时间
     */
    private LocalDateTime lastUsedTime;
    
    // ========== 状态 ==========
    
    /**
     * 状态
     */
    private String status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    // ========== 文档列表（详情时返回） ==========
    
    /**
     * 文档列表
     */
    private List<DocumentVO> documents;
    
    /**
     * 文档视图对象
     */
    @Data
    public static class DocumentVO {
        private Long id;
        private String fileName;
        private String fileType;
        private Long fileSize;
        private String fileSizeFormatted;
        private Integer segmentCount;
        private Integer vectorDimension;
        private Integer status;
        private String processingStatus;
        private String errorMessage;
        private LocalDateTime uploadTime;
        private LocalDateTime processTime;
    }
}
