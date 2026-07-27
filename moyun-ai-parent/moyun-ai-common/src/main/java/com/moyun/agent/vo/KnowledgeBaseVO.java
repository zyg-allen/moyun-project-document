package com.moyun.agent.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识库视图对象
 *
 * <p>用于前端展示的知识库信息，包含格式化后的文件大小和状态文本</p>
 *
 * @author laomao
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class KnowledgeBaseVO {
    private Long id;
    private String fileName;
    private String filePath;
    private String fileSize; // 格式化后的文件大小
    private String fileType;
    private Integer segmentCount;
    private Integer vectorDimension;
    private Integer status;
    private String statusText; // 状态文本
    private String processingStatus; // 处理状态：pending, configured, processing, completed, failed
    private Boolean configCompleted; // 是否完成配置
    private String errorMessage;
    private LocalDateTime uploadTime;
    private LocalDateTime processTime;
    
    // 新增字段
    private String category; // 分组
    private String tags; // 标签（JSON字符串）
    private String description; // 描述
    private Integer usageCount; // 使用次数
    private Integer hitCount; // 命中次数
    private LocalDateTime lastUsedTime; // 最后使用时间
    
    /**
     * 文档解析方式
     * <p>可能的值：POI, PDFBox, Text, LibreOffice</p>
     */
    private String parseMethod;
}
