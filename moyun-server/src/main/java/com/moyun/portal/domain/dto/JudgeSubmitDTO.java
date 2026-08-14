package com.moyun.portal.domain.dto;

import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * OJ 判题提交 DTO（v6.3 OJ 判题系统）
 *
 * @author moyun
 */
@Data
public class JudgeSubmitDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "题目ID不能为空")
    private Long questionId;

    @NotBlank(message = "代码不能为空")
    @Size(min = 1, max = 200_000, message = "代码长度不能超过 200KB")
    private String code;

    /** 编程语言：javascript/typescript/python/java/go/cpp/rust */
    @NotBlank(message = "编程语言不能为空")
    @Size(max = 20, message = "语言字段长度不能超过20个字符")
    private String language;
}
