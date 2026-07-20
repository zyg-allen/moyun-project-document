package com.moyun.portal.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 话题观点 VO
 *
 * @author moyun
 */
@Data
@Schema(description = "话题观点VO")
public class TopicPostVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "观点ID")
    private Long id;

    @Schema(description = "所属话题ID")
    private Long topicId;

    @Schema(description = "发布者ID")
    private Long userId;

    @Schema(description = "发布者用户名")
    private String username;

    @Schema(description = "发布者昵称")
    private String nickname;

    @Schema(description = "发布者头像")
    private String avatar;

    @Schema(description = "观点内容（Markdown）")
    private String content;

    @Schema(description = "图片 URL 列表，最多 9 张")
    private List<String> images;

    @Schema(description = "父观点 ID（楼中楼，NULL 为一级观点）")
    private Long parentPostId;

    @Schema(description = "回复的用户 ID")
    private Long replyToUserId;

    @Schema(description = "回复的用户昵称")
    private String replyToNickname;

    @Schema(description = "楼层号")
    private Integer floor;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "评论数")
    private Integer commentCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "当前用户是否已点赞该观点")
    private Boolean isLiked;

    @Schema(description = "当前用户是否为发布者")
    private Boolean isOwner;

    @Schema(description = "是否已软删：0 否/1 是")
    private Integer isDeleted;
}
