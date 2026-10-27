package com.moyun.ledger.domain.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 存钱期次标记：1=已存 2=未完成 3=跳过 0=重置 */
@Data
public class SavingMarkDTO {

    @NotNull(message = "标记状态不能为空")
    @Min(value = 0, message = "标记状态不合法")
    @Max(value = 3, message = "标记状态不合法")
    private Integer status;

    @DecimalMin(value = "0.01", message = "金额必须大于 0")
    private BigDecimal amount;

    @Size(max = 200, message = "原因最多 200 字")
    private String reason;
}
