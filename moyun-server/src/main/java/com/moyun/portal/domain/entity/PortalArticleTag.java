package com.moyun.portal.domain.entity;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

@Data
@TableName("portal_article_tag")
public class PortalArticleTag extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotNull(message = "文章ID不能为空")
    private Long articleId;

    /** 文章业务主键（关联 portal_article.business_id，双轨过渡） */
    private String articleBusinessId;

    @NotNull(message = "标签ID不能为空")
    private Long tagId;

    /** 标签业务主键（关联 portal_tag.business_id，双轨过渡） */
    private String tagBusinessId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    // 覆盖 BaseEntity 的 delFlag：本表无 del_flag 列（迁移脚本排除），保持物理删除（toggle/流水语义）
    @TableField(exist = false)
    private String delFlag;

    public PortalArticleTag()
    {
    }

    public PortalArticleTag(Long id)
    {
        this.id = id;
    }

    @Override
    public LocalDateTime getCreateTime()
    {
        return createTime;
    }

    @Override
    public void setCreateTime(LocalDateTime createTime)
    {
        this.createTime = createTime;
    }
}
