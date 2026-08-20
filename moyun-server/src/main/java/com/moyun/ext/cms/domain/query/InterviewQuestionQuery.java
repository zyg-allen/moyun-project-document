package com.moyun.ext.cms.domain.query;

import java.io.Serializable;

import lombok.Data;
import lombok.EqualsAndHashCode;

import com.moyun.core.base.page.PageDomain;

/**
 * 题目查询对象
 *
 * @author moyun
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class InterviewQuestionQuery extends PageDomain implements Serializable {
    private static final long serialVersionUID = 1L;

    private String keyword;           // 标题/描述模糊搜索

    private Long categoryId;          // 分类ID

    private String difficulty;        // 难度

    private Long companyId;           // 公司ID

    private String status;            // 状态

    /**
     * 题目类型：bagwen/algorithm/system_design/project/hr
     * 用于题库按类型筛选（v6.3 题目结构化）
     */
    private String questionType;

    /**
     * 练习模式：reading/choice/coding（v10.6 题库重构·阶段2）
     * 与 questionType 正交：questionType 描述内容分类，practiceMode 描述作答方式
     */
    private String practiceMode;
}
