package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 债权债务统一表：应收（别人欠我）与应付（我欠别人）同一张表，用 type 区分。
 *
 * <p>设计动机：应收/应付本质是同一类「往来」，只是方向相反、风险不同。分两张表
 * 意味着两套 CRUD / 两套统计 / 两套扩展；统一成一张表 + type 字段，一套代码覆盖，
 * 统计按 type 归组即可，后期加「投资」也只需扩展 type 枚举与风险字段。</p>
 *
 * <p>余额 = total_amount - paid_amount（不落列，避免三方不一致）；
 * settle_status 由二者推导后冗余存储，便于列表直接筛选。</p>
 *
 * @author moyun
 */
@Data
@TableName("ledger_obligation")
public class LedgerObligation {

    /** 类型：应收（计入资产） */
    public static final String TYPE_RECEIVABLE = "receivable";
    /** 类型：应付（计入负债） */
    public static final String TYPE_PAYABLE = "payable";

    /** 来源：手工录入 */
    public static final String SOURCE_MANUAL = "manual";
    /** 来源：销售赊账（库存模块联动） */
    public static final String SOURCE_SALE_CREDIT = "sale_credit";
    /** 来源：借出（记一笔的「借出」类型自动创建） */
    public static final String SOURCE_LEND = "lend";
    /** 来源：采购赊账（库存模块联动） */
    public static final String SOURCE_PURCHASE_CREDIT = "purchase_credit";
    /** 来源：借款（记一笔的「借款」类型自动创建） */
    public static final String SOURCE_BORROW = "borrow";

    /** 结算状态：未结 */
    public static final String SETTLE_PENDING = "pending";
    /** 结算状态：部分结 */
    public static final String SETTLE_PARTIAL = "partial";
    /** 结算状态：已结清 */
    public static final String SETTLE_SETTLED = "settled";

    /** 行状态：正常 */
    public static final int STATUS_NORMAL = 1;
    /** 行状态：已删除 */
    public static final int STATUS_DELETED = 0;

    private Long id;
    private Long userId;
    /** 类型：receivable=应收 / payable=应付（后期可加 investment） */
    private String type;
    private String counterparty;
    private Long counterpartyId;
    private Long warehouseId;
    private String sourceType;
    private Long sourceId;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private String settleStatus;
    private LocalDate dueDate;
    private String remark;
    private Integer status;
    private String createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
