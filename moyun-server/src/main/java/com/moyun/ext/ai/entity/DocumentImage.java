package com.moyun.ext.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档图片实体类
 *
 * <p>对应数据库表 document_image，存储从文档中提取的图片信息</p>
 *
 * @author laomao
 */
@Data
@TableName("document_image")
public class DocumentImage {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 关联的知识库ID
    private Long knowledgeBaseId;

    // 图片文件路径
    private String imagePath;

    // 所在页码
    private Integer pageNumber;

    // 图片在页面中的索引（一页可能有多张图）
    private Integer imageIndex;

    // 向量ID
    private String embeddingId;

    // 向量维度
    private Integer vectorDimension;

    // 图片宽度
    private Integer width;

    // 图片高度
    private Integer height;

    // 图片内容描述（多模态模型生成）
    private String description;

    // 描述语言
    private String descriptionLanguage;

    // 创建时间
    private LocalDateTime createTime;
}
