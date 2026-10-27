package com.moyun.ledger.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 关联已有流水 */
@Data
public class SavingLinkDTO {

    @NotNull(message = "请选择要关联的流水")
    private Long transactionId;
}
