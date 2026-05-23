package com.moyun.community.content.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArticleDetailDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long articleId;

    private String title;

    private String summary;

    private String coverUrl;

    private String content;

    private String contentType;

    private String articleType;

    private String sourceType;

    private Integer wordCount;

    private Integer readingTime;

    private String status;

    private Integer isTop;

    private Integer isRecommend;

    private Integer allowComment;

    private LocalDateTime publishTime;

    private Long viewCount;

    private Long likeCount;

    private Long collectCount;

    private Integer commentCount;

    private Integer shareCount;

    private AuthorInfo author;

    private CategoryInfo category;

    private List<TagInfo> tags;

    private Boolean isLiked;

    private Boolean isCollected;

    @Data
    public static class AuthorInfo implements Serializable {
        private Long userId;
        private String nickname;
        private String avatar;
        private Integer articleCount;
        private Integer followerCount;
        private Integer isAuthor;
    }

    @Data
    public static class CategoryInfo implements Serializable {
        private Long categoryId;
        private String categoryName;
        private String categoryCode;
        private String categoryType;
    }

    @Data
    public static class TagInfo implements Serializable {
        private Long tagId;
        private String tagName;
        private String tagColor;
    }
}
