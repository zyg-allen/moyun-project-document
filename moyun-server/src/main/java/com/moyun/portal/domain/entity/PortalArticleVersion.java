package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.moyun.core.base.BaseEntity;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章版本快照
 * <p>
 * 保存文章时生成版本快照，支持版本列表、详情、回滚、对比。
 *
 * @author moyun
 */
@Data
@TableName("portal_article_version")
public class PortalArticleVersion extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文章ID */
    private Long articleId;

    /** 文章业务主键（关联 portal_article.business_id，双轨过渡） */
    private String articleBusinessId;

    /** 版本号（同一文章内自增） */
    private Integer versionNo;

    /** 版本标题快照 */
    private String title;

    /** 版本内容快照（HTML） */
    private String content;

    /** 版本 Markdown 原始内容快照 */
    private String contentMarkdown;

    /** 版本摘要快照 */
    private String excerpt;

    /** 操作人ID（保存/回滚的执行者） */
    private Long operatorId;

    /** 操作人业务主键（关联 portal_user.business_id，双轨过渡） */
    private String operatorBusinessId;

    /** 版本创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    // BaseEntity 公共字段对应列在 portal_article_version 表中不存在，排除 MyBatis-Plus 映射，避免 SELECT/INSERT 报未知列
    @TableField(exist = false)
    private String createBy;
    @TableField(exist = false)
    private LocalDateTime createTime;
    @TableField(exist = false)
    private String updateBy;
    @TableField(exist = false)
    private LocalDateTime updateTime;
    @TableField(exist = false)
    private String remark;
}
