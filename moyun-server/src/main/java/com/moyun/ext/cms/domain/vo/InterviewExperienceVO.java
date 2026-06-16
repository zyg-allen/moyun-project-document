package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 面经 VO
 *
 * @author moyun
 */
@Data
public class InterviewExperienceVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private String title;

    private String company;

    private String position;

    private Integer year;

    private Integer month;

    private String summary;

    private String content;

    private String coverImage;

    /** 标签字符串（逗号分隔） */
    private List<String> tags;

    /** 通用标签（来自 portal_tag 关联） */
    private List<com.moyun.portal.domain.vo.TagVO> tagList;

    /** 是否置顶 */
    private Boolean isTop;

    private Long viewCount;

    private Long likeCount;

    private Long commentCount;

    /** 状态：draft/pending/published/rejected/archived */
    private String status;

    /** 作者信息 */
    private String userNickname;

    private String userAvatar;

    /** 当前用户是否已点赞 */
    private Boolean liked;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
