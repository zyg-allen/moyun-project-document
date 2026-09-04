package com.moyun.ledger.domain.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 记账创建/修改 DTO
 *
 * @author moyun
 */
@Data
public class TransactionCreateDTO {

    /** 记账类型：income/expense/transfer/repayment/borrow/adjust */
    @NotNull(message = "记账类型不能为空")
    private String type;

    /** 金额（元；adjust 可为负，其余必须大于0） */
    @NotNull(message = "金额不能为空")
    private BigDecimal amount;

    /** 分类ID */
    private Long categoryId;

    /** 关联资产账户ID（还款扣款方/转出方/校准账户；borrow 信用卡消费可空） */
    private Long accountId;

    /** 关联负债账户ID（还款/借款必填） */
    private Long liabilityId;

    /** 转账目标资产账户ID（transfer 必填） */
    private Long targetAccountId;

    /** 备注 */
    private String description;

    /** 交易日期（默认当天） */
    private LocalDate transactionDate;

    /** 商户名称 */
    private String merchant;

    /** 凭证截图URL（门户文件服务地址，选填） */
    private String voucherUrl;

    /** 是否计入预算（adjust 恒为0，其余默认1） */
    private Integer isBudget;

    /** 客户端幂等键（Phase 4 离线同步） */
    private String clientUuid;
}
