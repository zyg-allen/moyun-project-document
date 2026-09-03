package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 记账-净资产每日快照（趋势图与首页涨跌标识的数据来源）
 *
 * @author moyun
 */
@Data
@TableName("ledger_net_worth_snapshot")
public class LedgerNetWorthSnapshot {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 门户用户ID（portal_user.id） */
    private Long userId;

    /** 快照日期（每日定时任务生成；当日有记账实时upsert） */
    private LocalDate snapDate;

    /** 总资产（分） */
    private Long totalAsset;

    /** 总负债（分） */
    private Long totalLiability;

    /** 净资产（分）= total_asset - total_liability */
    private Long netWorth;

    private LocalDateTime createTime;
}
