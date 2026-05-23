package com.moyun.community.content.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArticleListVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long articleId;

    private String title;

    private String summary;

    private String coverUrl;

    private String articleType;

    private String categoryName;

    private Long categoryId;

    private String authorNickname;

    private Long authorId;

    private String authorAvatar;

    private Integer wordCount;

    private Integer readingTime;

    private String status;

    private Integer isTop;

    private Integer isRecommend;

    private LocalDateTime publishTime;

    private Long viewCount;

    private Long likeCount;

    private Integer commentCount;

    private List<String> tagNames;

    private Boolean isLiked;

    private Boolean isCollected;
}
