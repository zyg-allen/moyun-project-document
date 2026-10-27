package com.moyun.ledger.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 存钱期次创建转账（账户缺省取计划绑定账户） */
@Data
public class SavingTransferDTO {

    private Long accountId;
    private Long targetAccountId;

    @DecimalMin(value = "0.01", message = "金额必须大于 0")
    private BigDecimal amount;

    private LocalDate transactionDate;

    @Size(max = 200, message = "备注最多 200 字")
    private String remark;
}
