package com.moyun.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文档分片实体类
 *
 * <p>对应数据库表 document_segment，存储知识库文档的分片内容</p>
 *
 * @author laomao
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("document_segment")
public class DocumentSegment {
    @TableId(type = IdType.AUTO)
    private Long id;

    // 关联的知识库ID
    @TableField("knowledge_base_id")
    private Long knowledgeBaseId;

    // 分片索引（第几个分片）
    @TableField("segment_index")
    private Integer segmentIndex;

    // PDF页码
    @TableField("page_number")
    private Integer pageNumber;

    // 起始行号
    @TableField("line_start")
    private Integer lineStart;

    // 结束行号
    @TableField("line_end")
    private Integer lineEnd;

    // 起始字符位置
    @TableField("char_start")
    private Integer charStart;

    // 结束字符位置
    @TableField("char_end")
    private Integer charEnd;

    // 章节标题
    @TableField("chapter_title")
    private String chapterTitle;

    // 分片内容
    @TableField("content")
    private String content;

    // 分片内容长度
    @TableField("content_length")
    private Integer contentLength;

    // 向量ID（在Pinecone中的ID）
    @TableField("embedding_id")
    private String embeddingId;

    // 向量维度
    @TableField("vector_dimension")
    private Integer vectorDimension;

    // 向量数据（JSON格式存储，可选）
    @TableField("vector_data")
    private String vectorData;

    // 创建时间
    @TableField("create_time")
    private LocalDateTime createTime;
}
