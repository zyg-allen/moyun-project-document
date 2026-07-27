package com.moyun.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识库文档实体类
 *
 * <p>对应数据库表 knowledge_base，存储知识库中的文档信息和处理状态</p>
 * <p>一个知识库(KnowledgeLibrary)可以包含多个文档(KnowledgeBase)</p>
 *
 * @author laomao
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("knowledge_base")
public class KnowledgeBase {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 所属知识库ID
     */
    private Long libraryId;

    // 文件名
    private String fileName;

    // 文件路径（原始文件）
    private String filePath;

    // PDF 文件路径（用于预览）
    private String pdfFilePath;

    // 文件大小（字节）
    private Long fileSize;

    // 文件类型
    private String fileType;

    // 文档分段数量
    private Integer segmentCount;

    // 向量维度
    private Integer vectorDimension;

    // 处理状态：0-待处理，1-处理中，2-处理成功，3-处理失败
    private Integer status;

    // 处理状态（新）：pending(待配置), configured(已配置), processing(处理中), completed(已完成), failed(失败)
    private String processingStatus;

    // 是否完成配置
    private Boolean configCompleted;

    // 错误信息
    private String errorMessage;

    // 上传时间
    private LocalDateTime uploadTime;

    // 处理时间
    private LocalDateTime processTime;

    // ========== 新增：分组和标签 ==========
    
    // 知识库分组（如：技术文档、产品手册、FAQ等）
    private String category;
    
    // 知识库标签（JSON数组格式，如：["Java", "Spring", "后端"]）
    private String tags;
    
    // 知识库描述
    private String description;
    
    // ========== 新增：统计信息 ==========
    
    // 使用次数（被检索的次数）
    private Integer usageCount;
    
    // 命中次数（检索命中的次数）
    private Integer hitCount;
    
    // 最后使用时间
    private LocalDateTime lastUsedTime;

    // ========== 新增：解析方式 ==========
    
    /**
     * 文档解析方式
     * <ul>
     *     <li>POI - 使用 Apache POI 解析 (Word/Excel/PPT)</li>
     *     <li>PDFBox - 使用 Apache PDFBox 解析 (PDF)</li>
     *     <li>Text - 纯文本解析</li>
     *     <li>LibreOffice - 使用 LibreOffice 转换后解析</li>
     * </ul>
     */
    private String parseMethod;

    // ========== 新增：增量更新支持 ==========
    
    /**
     * 文档内容哈希值（用于检测变更）
     * <p>使用SHA-256计算文件内容的哈希值</p>
     */
    private String contentHash;
    
    /**
     * 上次处理时间（用于增量更新比较）
     */
    private java.time.LocalDateTime lastProcessedTime;
    
    /**
     * 是否需要重新处理
     */
    private Boolean needReprocess;
}
