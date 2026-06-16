package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 面试公司标签 VO
 *
 * @author moyun
 */
@Data
public class InterviewCompanyVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String slug;
    private String logo;
    private String description;
    private String industry;
    private Integer questionCount;
    private Integer sort;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
