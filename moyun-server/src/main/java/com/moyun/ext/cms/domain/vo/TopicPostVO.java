package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 话题下的动态（聚合带该话题标签的文章）
 *
 * @author moyun
 */
@Data
public class TopicPostVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String title;

    private String cover;

    private String excerpt;

    private Long authorId;

    private Long viewCount;

    private Long likeCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    /** 作者昵称 */
    private String authorName;

    /** 作者头像 */
    private String authorAvatar;
}
