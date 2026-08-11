package com.moyun.ext.ai.entity;

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
 * <p>继承 {@link AiBaseEntity}，复用 createTime / updateTime / deleted 字段（P3-2 Phase 2）：
 * <ul>
 *   <li>{@code createTime} → {@code create_time} 列（原 {@code upload_time}，117 脚本重命名，
 *       语义：记录创建=文档上传时刻）</li>
 *   <li>{@code updateTime} → {@code update_time} 列（原 {@code process_time}，117 脚本重命名，
 *       语义扩展为"最后修改时间"；处理路径仍由 ServiceImpl 显式 setUpdateTime 保留"最近处理"语义）</li>
 *   <li>{@code deleted} → {@code deleted} 列（由 113 脚本添加）</li>
 * </ul>
 * 注意：{@code lastProcessedTime}（对应 {@code last_processed_time} 列）为独立语义字段，
 * 不参与本次重命名，继续保留为本类字段。
 * 时间字段的 {@code @TableField(fill=...)} 自动填充由 AiBaseEntity 统一提供，
 * strictInsertFill 仅填 null，不覆盖 Service 层显式赋值（P0-1 兜底兼容）。</p>
 *
 * @author laomao
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("ai_knowledge_base")
public class KnowledgeBase extends AiBaseEntity {
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

    // createTime（原 upload_time，117 脚本重命名，记录创建=文档上传时刻）
    // updateTime（原 process_time，117 脚本重命名，最后修改时间）
    // 两个字段继承自 AiBaseEntity，不再在此声明

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

    // deleted 字段继承自 AiBaseEntity（对应 deleted 列，由 113 脚本添加）
}
