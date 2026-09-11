package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 专栏文章目录简要信息
 *
 * @author moyun
 */
@Data
public class ArticleSimpleVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String title;

    private String cover;

    private String excerpt;

    private Long viewCount;

    private Long likeCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    /** 专栏内顺序 */
    private Integer sortOrder;

    /** 作者用户ID（CMS后台维护文章/批量加入专栏弹窗展示用） */
    private Long userId;

    /** 作者昵称（CMS后台展示用，由 Mapper 关联 portal_user 填充） */
    private String authorName;

    /** 作者用户名（CMS后台展示用，由 Mapper 关联 portal_user 填充） */
    private String authorUsername;
}
