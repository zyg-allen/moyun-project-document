package com.moyun.ext.cms.domain.query;

import java.io.Serializable;

import lombok.Data;
import lombok.EqualsAndHashCode;

import com.moyun.core.base.page.PageDomain;

/**
 * 公司标签查询对象
 *
 * @author moyun
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class InterviewCompanyQuery extends PageDomain implements Serializable {
    private static final long serialVersionUID = 1L;

    private String keyword;           // 名称/简介模糊搜索

    private String industry;          // 行业

    private String status;            // 状态
}
