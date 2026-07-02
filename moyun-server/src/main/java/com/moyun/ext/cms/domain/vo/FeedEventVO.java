package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 动态事件 VO
 *
 * @author moyun
 */
@Data
public class FeedEventVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 动态事件ID */
    private Long eventId;

    /** 事件发布者ID */
    private Long userId;

    /** 发布者昵称 */
    private String userNickname;

    /** 发布者头像 */
    private String userAvatar;

    /** 事件类型：publish_article/publish_experience/new_column/checkin 等 */
    private String eventType;

    /** 目标类型：article/experience/column/book 等 */
    private String targetType;

    /** 目标对象ID */
    private Long targetId;

    /** 目标标题 */
    private String title;

    /** 动态摘要 */
    private String summary;

    /** 封面图 */
    private String cover;

    /** 事件创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    /** 当前用户是否已点赞（预留，暂不实现） */
    private Boolean isLiked;
}
