package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 专栏详情 VO
 *
 * @author moyun
 */
@Data
public class ColumnVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private String title;

    private String subtitle;

    private String description;

    private String cover;

    private Long categoryId;

    private String status;

    private Integer articleCount;

    private Integer subscribeCount;

    private Integer viewCount;

    private Integer isFinished;

    private BigDecimal price;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;

    // ==================== 作者信息 ====================

    /** 作者昵称 */
    private String authorName;

    /** 作者头像 */
    private String authorAvatar;

    /** 作者简介 */
    private String authorBio;

    // ==================== 当前用户视角 ====================

    /** 当前用户是否已订阅 */
    private Boolean isSubscribed;

    /** 文章目录 */
    private List<ArticleSimpleVO> articles;
}
