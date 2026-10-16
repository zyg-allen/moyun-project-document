package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 债务核销流水：每一次还款/收款都留痕。
 *
 * <p>为什么不只累加 paid_amount：</p>
 * <ol>
 *   <li>可追溯——用户要知道这笔钱分几次、每次多少、走的哪个账户、谁操作的；</li>
 *   <li>可撤销——删掉一条核销记录即可回滚 paid_amount，不必猜原始值；</li>
 *   <li>与资金流水对账时，它是唯一的中间凭据。</li>
 * </ol>
 *
 * @author moyun
 */
@Data
@TableName("ledger_debt_repayment")
public class LedgerDebtRepayment {

    /** 债务类型：应收（对方还我钱） */
    public static final String DEBT_RECEIVABLE = "receivable";
    /** 债务类型：应付（我还对方钱） */
    public static final String DEBT_PAYABLE = "payable";

    /** 行状态：正常 */
    public static final int STATUS_NORMAL = 1;
    /** 行状态：已撤销 */
    public static final int STATUS_REVOKED = 0;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 冗余 userId，便于直接按用户查全部核销记录 */
    private Long userId;

    /** receivable / payable */
    private String debtType;

    /** 对应 ledger_receivable.id 或 ledger_payable.id */
    private Long debtId;

    private BigDecimal amount;

    /** 资金账户ID（钱进/出哪个账户） */
    private Long accountId;

    /** 关联的资金流水ID */
    private Long transactionId;

    private LocalDate repayDate;

    /** 操作人用户ID（多人协作用于审计） */
    private Long operatorId;

    private String remark;

    private Integer status;

    private LocalDateTime createTime;
}