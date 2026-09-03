package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 记账-预算（category_id NULL=月度总预算，非空=分类预算）
 *
 * @author moyun
 */
@Data
@TableName("ledger_budget")
public class LedgerBudget {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 门户用户ID（portal_user.id） */
    private Long userId;

    /** 分类ID（NULL=月度总预算，非空=分类预算） */
    private Long categoryId;

    /** 年份 */
    private Integer year;

    /** 月份（1-12） */
    private Integer month;

    /** 预算金额（分） */
    private Long amount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
