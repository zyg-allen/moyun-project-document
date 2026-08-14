package com.moyun.portal.domain.dto;

import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 测试用例新增/修改 DTO（CMS 后台使用，v6.3 OJ 判题系统）
 *
 * @author moyun
 */
@Data
public class TestCaseUpsertDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "题目ID不能为空")
    private Long questionId;

    @Size(max = 100_000, message = "输入不能超过 100KB")
    private String input;

    @NotBlank(message = "期望输出不能为空")
    @Size(max = 100_000, message = "期望输出不能超过 100KB")
    private String expectedOutput;

    /** 是否样例：true/false */
    private Boolean isSample;

    /** 用例排序 */
    private Integer orderNum;

    @Size(max = 1000, message = "说明长度不能超过 1000 字符")
    private String explanation;
}
