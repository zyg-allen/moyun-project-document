package com.moyun.ledger.domain.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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

    /** 二级分类ID（未选二级为 null） */
    private Long subCategoryId;

    /** 关联资产账户ID（还款扣款方/转出方/校准账户；borrow 信用卡消费可空） */
    private Long accountId;

    /** 关联负债账户ID（还款/借款必填） */
    private Long liabilityId;

    /** 关联应收款ID（借出/收款时填写，用于双向追溯） */
    private Long receivableId;

    /** 关联应付款ID（借款/还款时填写） */
    private Long payableId;

    /** 转账目标资产账户ID（transfer 必填） */
    private Long targetAccountId;

    /** 备注 */
    @Size(max = 200, message = "备注最多 200 字")
    private String description;

    /** 交易日期（默认当天） */
    private LocalDate transactionDate;

    /** 交易时间 HH:mm[:ss]（为空时服务端取当前时间） */
    @Pattern(regexp = "^$|^([01]\\d|2[0-3]):[0-5]\\d(:[0-5]\\d)?$", message = "交易时间格式应为 HH:mm 或 HH:mm:ss")
    private String transactionTime;

    /** 创建人（服务端从登录态填充，前端勿传；修改时保留原创建人） */
    private String createBy;

    /** 商户名称 */
    @Size(max = 100, message = "商户名称最多 100 字")
    private String merchant;

    /** 对方名称（借出时必填/选填：用于自动生成应收款） */
    private String counterparty;

    /** 凭证截图URL（门户文件服务地址，选填） */
    private String voucherUrl;

    /** 是否计入预算（adjust 恒为0，其余默认1） */
    private Integer isBudget;

    /** 客户端幂等键（Phase 4 离线同步） */
    private String clientUuid;

    /** 业务来源（服务端设置，如 saving=存钱转入） */
    private String bizType;
}
