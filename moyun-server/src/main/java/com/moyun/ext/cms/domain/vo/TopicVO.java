package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 话题详情 VO
 *
 * @author moyun
 */
@Data
public class TopicVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String slug;

    private String description;

    private String cover;

    private Integer postCount;

    private Integer followCount;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    /** 当前用户是否已关注（未登录为 false） */
    private Boolean isFollowed;
}
