package com.moyun.ext.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 参考来源反馈实体
 *
 * <p>对应数据库表 reference_feedback，存储用户对RAG检索结果的反馈</p>
 *
 * @author laomao
 */
@Data
@TableName("reference_feedback")
public class ReferenceFeedback {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 知识库ID
     */
    private Long knowledgeBaseId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 页码
     */
    private Integer pageNumber;

    /**
     * 分片索引
     */
    private Integer segmentIndex;

    /**
     * 用户查询
     */
    private String userQuery;

    /**
     * 重排分数
     */
    private Double rerankScore;

    /**
     * 向量相似度
     */
    private Double vectorScore;

    /**
     * 反馈类型：accurate(准确), inaccurate(不准确)
     */
    private String feedbackType;

    /**
     * 智能体ID
     */
    private Long agentId;

    /**
     * 会话ID
     */
    private String memoryId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
