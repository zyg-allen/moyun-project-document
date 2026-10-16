package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 应收款：别人欠我的钱。
 *
 * <p>余额 = total_amount - paid_amount（不落列，避免三方不一致）；
 * settle_status 由二者推导后冗余存储，便于列表直接筛选。</p>
 *
 * @author moyun
 */
@Data
@TableName("ledger_receivable")
public class LedgerReceivable {

    /** 来源：手工录入 */
    public static final String SOURCE_MANUAL = "manual";
    /** 来源：销售赊账（库存模块联动） */
    public static final String SOURCE_SALE_CREDIT = "sale_credit";
    /** 来源：借出（记一笔的「借出」类型自动创建） */
    public static final String SOURCE_LEND = "lend";

    /** 结算状态：未收 */
    public static final String SETTLE_PENDING = "pending";
    /** 结算状态：部分收 */
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

    /** 对方名称（欠款人） */
    private String counterparty;

    /** 对方用户ID（对方也是平台用户时填写） */
    private Long counterpartyId;

    /** 归属仓库ID（库存模块上线后使用；个人记账为 null） */
    private Long warehouseId;

    private String sourceType;

    private Long sourceId;

    /** 应收总额 */
    private BigDecimal totalAmount;

    /** 已收金额 */
    private BigDecimal paidAmount;

    private String settleStatus;

    private LocalDate dueDate;

    private String remark;

    private Integer status;

    private String createBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}