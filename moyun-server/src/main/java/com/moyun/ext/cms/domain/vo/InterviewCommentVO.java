package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 面经评论 VO（支持多级）
 *
 * @author moyun
 */
@Data
public class InterviewCommentVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long experienceId;

    private Long userId;

    private Long parentId;

    private Long replyToUserId;

    private String content;

    private Long likeCount;

    private Integer sort;

    private String status;

    /** 作者信息 */
    private String userNickname;

    private String userAvatar;

    /** 回复目标用户 */
    private String replyToUserNickname;

    /** 当前用户是否已点赞 */
    private Boolean liked;

    /** 子评论（多级评论） */
    private List<InterviewCommentVO> replies;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
