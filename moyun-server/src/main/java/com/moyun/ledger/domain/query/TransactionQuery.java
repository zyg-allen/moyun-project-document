package com.moyun.ledger.domain.query;

import lombok.Data;

/**
 * 流水列表查询条件
 *
 * @author moyun
 */
@Data
public class TransactionQuery {

    /** 页码（1 起） */
    private Integer pageNum = 1;

    /** 每页条数（默认20） */
    private Integer pageSize = 20;

    /** 类型筛选：income/expense/transfer/repayment/borrow/adjust */
    private String type;

    /** 资产账户ID筛选 */
    private Long accountId;

    /** 负债账户ID筛选 */
    private Long liabilityId;

    /** 分类ID筛选 */
    private Long categoryId;

    /** 起始日期（含） */
    private String startDate;

    /** 截止日期（含） */
    private String endDate;
}
