package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("portal_like")
public class PortalLike implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /** 用户业务主键（关联 portal_user.business_id，双轨过渡） */
    private String userBusinessId;

    @NotNull(message = "文章ID不能为空")
    private Long articleId;

    /** 文章业务主键（关联 portal_article.business_id，双轨过渡） */
    private String articleBusinessId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    public PortalLike() {
    }

    public PortalLike(Long id) {
        this.id = id;
    }
}
