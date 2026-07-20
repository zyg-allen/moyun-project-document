package com.moyun.portal.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 话题列表 VO（精简字段）
 *
 * @author moyun
 */
@Data
@Schema(description = "话题列表VO")
public class TopicListVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "话题ID")
    private Long id;

    @Schema(description = "话题标题")
    private String title;

    @Schema(description = "话题描述/导语")
    private String description;

    @Schema(description = "封面图 URL")
    private String cover;

    @Schema(description = "发起人ID")
    private Long creatorId;

    @Schema(description = "发起人昵称")
    private String creatorNickname;

    @Schema(description = "发起人头像")
    private String creatorAvatar;

    @Schema(description = "状态：active/archived")
    private String status;

    @Schema(description = "是否置顶：0 否/1 是")
    private Integer pinned;

    @Schema(description = "浏览数")
    private Integer viewCount;

    @Schema(description = "观点数")
    private Integer postCount;

    @Schema(description = "话题被赞数")
    private Integer likeCount;

    @Schema(description = "评论数")
    private Integer commentCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最后观点时间")
    private LocalDateTime lastPostTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createdTime;
}
