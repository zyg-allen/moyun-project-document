package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 记账-流水（核心表）
 *
 * <p>注意：不继承 BaseEntity；status 自管逻辑删除（冲正后归档，不物理删除），
 * 不使用 BaseEntity 的 delFlag/@TableLogic 机制。
 *
 * @author moyun
 */
@Data
@TableName("ledger_transaction")
public class LedgerTransaction {

    /** 类型：收入（资产+，净资产+） */
    public static final String TYPE_INCOME = "income";
    /** 类型：支出（资产-，净资产-） */
    public static final String TYPE_EXPENSE = "expense";
    /** 类型：转账（资产间流转，总资产不变） */
    public static final String TYPE_TRANSFER = "transfer";
    /** 类型：还款（资产- 负债-，净资产不变） */
    public static final String TYPE_REPAYMENT = "repayment";
    /** 类型：借款（资产+ 负债+，净资产不变；信用卡消费时 account_id 可为 NULL 仅负债+） */
    public static final String TYPE_BORROW = "borrow";
    /** 类型：余额校准（差额留痕，可正可负；不计预算） */
    public static final String TYPE_ADJUST = "adjust";

    /** 状态：正常 */
    public static final int STATUS_NORMAL = 1;
    /** 状态：已删除（冲正后归档） */
    public static final int STATUS_DELETED = 0;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 门户用户ID（portal_user.id） */
    private Long userId;

    /** 类型：income/expense/transfer/repayment/borrow/adjust */
    private String type;

    /** 金额（元；adjust 可为负表示调减，其余恒为正，方向由 type 决定） */
    private BigDecimal amount;

    /** 分类ID（ledger_category） */
    private Long categoryId;

    /** 关联资产账户（支出/收入/转出方/还款扣款方/校准账户） */
    private Long accountId;

    /** 关联负债账户（还款/借款） */
    private Long liabilityId;

    /** 转账目标资产账户 */
    private Long targetAccountId;

    /** 主账户交易后余额快照（元） */
    private BigDecimal balanceAfter;

    /** 转账目标账户交易后余额快照（元） */
    private BigDecimal targetBalanceAfter;

    /** 关联负债交易后欠款快照（元） */
    private BigDecimal liabilityBalanceAfter;

    /** 备注 */
    private String description;

    /** 交易日期（默认当天） */
    private LocalDate transactionDate;

    /** 交易时间 */
    private String transactionTime;

    /** 商户名称 */
    private String merchant;

    /** 凭证截图URL（门户文件服务地址） */
    private String voucherUrl;

    /** 是否计入预算：1=是 0=否（adjust 默认0） */
    private Integer isBudget;

    /** 状态：1=正常 0=已删除（冲正后归档） */
    private Integer status;

    /** 客户端幂等键（Phase 4 离线同步防重复提交） */
    private String clientUuid;

    /** 创建人（门户用户名；后台代改时为 admin 标识，数据隔离溯源用） */
    private String createBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    // ---------------- 列表展示用冗余字段（非表字段，分页查询后填充名称） ----------------

    /** 账户名称（展示用） */
    @TableField(exist = false)
    private String accountName;

    /** 转账目标账户名称（展示用） */
    @TableField(exist = false)
    private String targetAccountName;

    /** 负债名称（展示用） */
    @TableField(exist = false)
    private String liabilityName;

    /** 分类名称（展示用） */
    @TableField(exist = false)
    private String categoryName;
}
