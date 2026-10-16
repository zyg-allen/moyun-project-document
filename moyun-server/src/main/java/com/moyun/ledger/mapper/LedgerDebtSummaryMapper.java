package com.moyun.ledger.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

/**
 * 债务汇总查询（只读聚合）。
 *
 * <p>为什么单独一个 Mapper 而不复用实体 Mapper：这里要的是 `SUM(total - paid)` 的
 * 库内聚合，走实体查询会把全部债务行捞到内存再累加 —— 数据量上来后明显吃亏。</p>
 *
 * @author moyun
 */
@Mapper
public interface LedgerDebtSummaryMapper {

    /** 应收未结余额合计（别人还欠我的） */
    @Select("SELECT IFNULL(SUM(total_amount - paid_amount), 0) FROM ledger_receivable "
            + "WHERE user_id = #{userId} AND status = 1 AND settle_status <> 'settled'")
    BigDecimal sumReceivableOutstanding(@Param("userId") Long userId);

    /** 应付未结余额合计（我还欠别人的） */
    @Select("SELECT IFNULL(SUM(total_amount - paid_amount), 0) FROM ledger_payable "
            + "WHERE user_id = #{userId} AND status = 1 AND settle_status <> 'settled'")
    BigDecimal sumPayableOutstanding(@Param("userId") Long userId);
}