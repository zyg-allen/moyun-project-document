package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 应付款：我欠别人的钱。字段与 {@link LedgerReceivable} 对称。
 *
 * @author moyun
 */
@Data
@TableName("ledger_payable")
public class LedgerPayable {

    /** 来源：手工录入 */
    public static final String SOURCE_MANUAL = "manual";
    /** 来源：采购赊账（库存模块联动） */
    public static final String SOURCE_PURCHASE_CREDIT = "purchase_credit";
    /** 来源：借款（记一笔的「借款」类型自动创建） */
    public static final String SOURCE_BORROW = "borrow";

    /** 结算状态：未还 */
    public static final String SETTLE_PENDING = "pending";
    /** 结算状态：部分还 */
    public static final String SETTLE_PARTIAL = "partial";
    /** 结算状态：已结清 */
    public static final String SETTLE_SETTLED = "settled";

    /** 行状态：正常 */
    public static final int STATUS_NORMAL = 1;
    /** 行状态：已删除 */
    public static final int STATUS_DELETED = 0;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 对方名称（债权人） */
    private String counterparty;

    private Long counterpartyId;

    private Long warehouseId;

    private String sourceType;

    private Long sourceId;

    /** 应付总额 */
    private BigDecimal totalAmount;

    /** 已还金额 */
    private BigDecimal paidAmount;

    private String settleStatus;

    private LocalDate dueDate;

    private String remark;

    private Integer status;

    private String createBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}