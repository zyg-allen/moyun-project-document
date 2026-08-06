package com.moyun.portal.domain.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

@Data
@TableName("portal_comment")
public class PortalComment extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotNull(message = "文章ID不能为空")
    private Long articleId;

    private Long authorId;

    @NotBlank(message = "评论内容不能为空")
    private String content;

    private Long parentId;

    private Long rootId;

    private Long replyTo;

    private String replyToContent;

    private Long likeCount;

    /**
     * 状态：0=待审核 1=已发布 2=审核驳回
     * 与文章审核模式对齐，CMS 后台可审核评论
     */
    private String status;

    /** 审核人ID（sys_user.user_id，CMS审核时写入） */
    private Long auditorId;

    /** 审核意见/驳回原因（独立字段，专用于审核记录） */
    private String auditRemark;

    /** 审核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;

    public PortalComment()
    {
    }

    public PortalComment(Long id)
    {
        this.id = id;
    }
}
