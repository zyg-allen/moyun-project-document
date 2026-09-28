package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 面试分类 VO（前后端字段对齐核心
 *
 * @author moyun
 */
@Data
public class InterviewCategoryVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String slug;

    /** 题库类型：interview/certification/civil-service/postgraduate/other */
    private String bankType;

    /** 上级分类ID（0=顶级） */
    private Long parentId;

    /** 职业族群：后端/前端/测试/产品/运维… */
    private String jobFamily;

    private String description;

    private String icon;

    private Integer sort;

    private Integer questionCount;

    private String status;

    /** 前台展示：0=展示 1=隐藏 */
    private String visible;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
