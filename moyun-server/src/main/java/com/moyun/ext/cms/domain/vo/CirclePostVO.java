package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 圈子帖子 VO
 *
 * @author moyun
 */
@Data
public class CirclePostVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long circleId;

    private Long userId;

    private String title;

    private String content;

    private Integer viewCount;

    private Integer likeCount;

    private Integer replyCount;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    /** 发帖人昵称 */
    private String authorName;

    /** 发帖人头像 */
    private String authorAvatar;
}
