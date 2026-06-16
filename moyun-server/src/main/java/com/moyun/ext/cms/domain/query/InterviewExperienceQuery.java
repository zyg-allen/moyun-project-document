package com.moyun.ext.cms.domain.query;

import java.io.Serializable;

import lombok.Data;
import lombok.EqualsAndHashCode;

import com.moyun.core.base.page.PageDomain;

/**
 * 面经查询对象
 *
 * @author moyun
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class InterviewExperienceQuery extends PageDomain implements Serializable {
    private static final long serialVersionUID = 1L;

    private String keyword;           // 标题/内容模糊搜索

    private String company;           // 公司

    private Integer year;             // 年份

    private Long userId;              // 作者用户ID

    private String status;            // 状态
}
