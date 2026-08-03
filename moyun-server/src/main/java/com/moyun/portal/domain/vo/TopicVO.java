package com.moyun.portal.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 话题详情 VO
 *
 * <p>含创建者信息、统计字段、当前用户与该话题的互动状态（isLiked / isOwner）。</p>
 *
 * @author moyun
 */
@Data
@Schema(description = "话题详情VO")
public class TopicVO implements Serializable {

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

    @Schema(description = "发起人用户名")
    private String creatorUsername;

    @Schema(description = "发起人昵称")
    private String creatorNickname;

    @Schema(description = "发起人头像")
    private String creatorAvatar;

    @Schema(description = "状态：pending 待审核/active 活跃/archived 归档/deleted 删除/rejected 审核驳回")
    private String status;

    @Schema(description = "审核人ID")
    private Long auditorId;

    @Schema(description = "审核意见/驳回原因")
    private String auditRemark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "审核时间")
    private LocalDateTime auditTime;

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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;

    @Schema(description = "当前用户是否已点赞该话题")
    private Boolean isLiked;

    @Schema(description = "当前用户是否为话题发起人")
    private Boolean isOwner;
}
