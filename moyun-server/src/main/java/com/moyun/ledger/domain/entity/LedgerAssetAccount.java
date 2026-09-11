package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 记账-资产账户
 *
 * <p>注意：不继承 BaseEntity（表无 create_by/update_by/del_flag 审计字段）；
 * 删除即 status=0 停用归档，流水永久保留。
 *
 * @author moyun
 */
@Data
@TableName("ledger_asset_account")
public class LedgerAssetAccount {

    /** 类型：现金 */
    public static final String TYPE_CASH = "cash";
    /** 类型：储蓄卡 */
    public static final String TYPE_SAVINGS = "savings";
    /** 类型：电子钱包 */
    public static final String TYPE_EWALLET = "ewallet";
    /** 类型：储值卡 */
    public static final String TYPE_STORED_VALUE = "stored_value";
    /** 类型：投资 */
    public static final String TYPE_INVESTMENT = "investment";
    /** 类型：固定资产 */
    public static final String TYPE_FIXED_ASSET = "fixed_asset";
    /** 类型：债权 */
    public static final String TYPE_RECEIVABLE = "receivable";
    /** 类型：其他 */
    public static final String TYPE_OTHER = "other";

    /** 状态：启用 */
    public static final int STATUS_ENABLED = 1;
    /** 状态：停用归档（删除即归档） */
    public static final int STATUS_ARCHIVED = 0;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 门户用户ID（portal_user.id） */
    private Long userId;

    /** 账户名称，如“招商银行储蓄卡” */
    private String name;

    /** 类型：cash/savings/ewallet/stored_value/investment/fixed_asset/receivable/other */
    private String type;

    /** 当前余额（元）。禁止直接编辑，校准必须走 adjust 记账 */
    private BigDecimal balance;

    /** 估值（元；投资/固定资产用，可≠balance） */
    private BigDecimal valuation;

    /** 是否计入总资产：1=是 0=否 */
    private Integer includeInTotal;

    /** 图标 */
    private String icon;

    /** 是否隐藏余额（隐私模式）：1=是 0=否 */
    private Integer hideBalance;

    /** 排序 */
    private Integer sortOrder;

    /** 状态：1=启用 0=停用归档 */
    private Integer status;

    /** 乐观锁版本号 */
    private Integer version;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
