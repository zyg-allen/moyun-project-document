package com.moyun.portal.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 话题评论 VO（含楼中楼回复列表）
 *
 * @author moyun
 */
@Data
@Schema(description = "话题评论VO")
public class TopicCommentVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "评论ID")
    private Long id;

    @Schema(description = "目标类型：topic / post")
    private String targetType;

    @Schema(description = "目标ID")
    private Long targetId;

    @Schema(description = "评论者ID")
    private Long authorId;

    @Schema(description = "评论者用户名")
    private String authorUsername;

    @Schema(description = "评论者昵称")
    private String authorNickname;

    @Schema(description = "评论者头像")
    private String authorAvatar;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "父评论ID（0=一级评论）")
    private Long parentId;

    @Schema(description = "根评论ID（一级评论 root_id=0）")
    private Long rootId;

    @Schema(description = "被回复用户ID")
    private Long replyTo;

    @Schema(description = "被回复用户名")
    private String replyToUsername;

    @Schema(description = "被回复昵称")
    private String replyToNickname;

    @Schema(description = "被回复内容摘要")
    private String replyToContent;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "回复数（仅一级评论维护）")
    private Integer replyCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "当前用户是否已点赞该评论")
    private Boolean isLiked;

    @Schema(description = "当前用户是否为评论者")
    private Boolean isOwner;

    @Schema(description = "子评论列表（仅一级评论填充）")
    private List<TopicCommentVO> replies;
}
