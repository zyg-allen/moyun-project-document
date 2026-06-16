package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 简历模板 VO
 *
 * @author moyun
 */
@Data
public class InterviewResumeTemplateVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String title;

    private String description;

    private String cover;

    private String downloadUrl;

    private String category;

    private String fileType;

    private Long fileSize;

    private Boolean isPremium;

    private String usageGuide;

    /** 标签字符串（逗号分隔） */
    private List<String> tags;

    /** 通用标签（来自 portal_tag 关联） */
    private List<com.moyun.portal.domain.vo.TagVO> tagList;

    private Long likeCount;

    private Long downloadCount;

    private Integer sort;

    private String status;

    /** 当前用户是否已点赞 */
    private Boolean liked;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
