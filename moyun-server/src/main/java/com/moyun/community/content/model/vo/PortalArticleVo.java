package com.moyun.community.content.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 前台文章VO
 *
 * @author moyun
 */
@Data
public class PortalArticleVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String title;
    private String content;
    private String contentMarkdown;
    private String excerpt;
    private String cover;
    private String category;
    private String categoryId;
    private List<String> tags;
    private String authorId;
    private String authorNickname;
    private String authorAvatar;
    private Integer views;
    private Integer likes;
    private Integer commentsCount;
    private Integer shareCount;
    private Integer bookmarkCount;
    private Boolean isFeatured;
    private Boolean isTop;
    private String status;
    private String createdAt;
    private String updatedAt;
    private String publishedAt;
    private String readingTime;
    private Integer wordCount;
    private Boolean isLiked;
    private Boolean isBookmarked;
}
