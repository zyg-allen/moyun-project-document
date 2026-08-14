package com.moyun.portal.domain.vo;

import java.io.Serializable;

import lombok.Data;

/**
 * 题目测试用例 VO（v6.3 OJ 判题系统）
 * <p>
 * 公共接口仅返回样例用例（is_sample=1），隐藏判题用例的字段值为空。
 *
 * @author moyun
 */
@Data
public class TestCaseVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long questionId;

    /** 标准输入（样例用例返回，隐藏用例为 null） */
    private String input;

    /** 期望输出（样例用例返回，隐藏用例为 null） */
    private String expectedOutput;

    /** 是否样例 */
    private Boolean isSample;

    /** 用例排序 */
    private Integer orderNum;

    /** 用例说明 */
    private String explanation;
}
