package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章浏览记录表
 * 用于防止刷阅读量
 */
@Data
@TableName("portal_article_view")
public class PortalArticleView implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotNull(message = "文章ID不能为空")
    private Long articleId;

    private Long userId;

    private String ip;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime viewTime;

    private String userAgent;

    public PortalArticleView() {
    }

    public PortalArticleView(Long articleId, String ip) {
        this.articleId = articleId;
        this.ip = ip;
        this.viewTime = LocalDateTime.now();
    }

    public PortalArticleView(Long articleId, Long userId, String ip) {
        this.articleId = articleId;
        this.userId = userId;
        this.ip = ip;
        this.viewTime = LocalDateTime.now();
    }
}
