package com.moyun.ext.cms.domain.query;

import java.io.Serializable;

import lombok.Data;
import lombok.EqualsAndHashCode;

import com.moyun.core.base.page.PageDomain;

/**
 * 简历模板查询对象
 *
 * @author moyun
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class InterviewResumeTemplateQuery extends PageDomain implements Serializable {
    private static final long serialVersionUID = 1L;

    private String keyword;           // 标题/描述模糊搜索

    private String category;          // 分类

    private String fileType;          // 文件类型

    private Boolean isPremium;        // 是否付费

    private String status;            // 状态
}
