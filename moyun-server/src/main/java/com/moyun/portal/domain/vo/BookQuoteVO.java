package com.moyun.portal.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 金句摘录视图对象
 * 包含出处书籍、摘录人信息
 *
 * @author moyun
 */
@Data
@Schema(description = "金句摘录VO")
public class BookQuoteVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "金句ID", example = "1")
    private Long id;

    @Schema(description = "摘录用户ID", example = "1")
    private Long userId;

    @Schema(description = "摘录用户昵称", example = "小明")
    private String userNickname;

    @Schema(description = "摘录用户头像", example = "https://example.com/avatar.jpg")
    private String userAvatar;

    @Schema(description = "书籍ID", example = "1")
    private Long bookId;

    @Schema(description = "书籍标题", example = "活着")
    private String bookTitle;

    @Schema(description = "书籍作者", example = "余华")
    private String bookAuthor;

    @Schema(description = "书籍封面", example = "https://example.com/cover.jpg")
    private String bookCover;

    @Schema(description = "金句内容", example = "人是为活着本身而活着的")
    private String content;

    @Schema(description = "页码", example = "第128页")
    private String page;

    @Schema(description = "章节", example = "第一章")
    private String chapter;

    @Schema(description = "位置描述", example = "第128页 第三章开头")
    private String location;

    @Schema(description = "点赞数", example = "42")
    private Long likeCount;

    @Schema(description = "是否公开", example = "true")
    private Boolean isPublic;

    @Schema(description = "是否精选", example = "true")
    private Boolean isFeatured;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "摘录时间", example = "2024-01-01 12:00:00")
    private LocalDateTime createTime;
}
