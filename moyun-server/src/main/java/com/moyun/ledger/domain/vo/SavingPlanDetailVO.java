package com.moyun.ledger.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Data;

/**
 * 存钱计划详情 VO。
 *
 * <p>替代原先直接返回 Entity 的 Map，显式收口对外字段：
 * 不暴露 userId、createTime/updateTime 等内部字段，避免接口随表结构变化。</p>
 */
@Data
public class SavingPlanDetailVO {

    private Plan plan;
    private List<Record> records;

    @Data
    public static class Plan {
        private Long id;
        private String name;
        private String method;
        private BigDecimal targetAmount;
        private BigDecimal currentAmount;
        private BigDecimal periodAmount;
        private Integer periodCount;
        private BigDecimal increaseStep;
        private LocalDate startDate;
        private LocalDate endDate;
        private String remark;
        private Integer status;
        private Long sourceAccountId;
        private Long savingsAccountId;
    }

    @Data
    public static class Record {
        private Long id;
        private Integer periodIndex;
        private BigDecimal targetAmount;
        private BigDecimal amount;
        private Integer status;
        private String failReason;
        private String remark;
        private LocalDate recordDate;
        private Long transactionId;
    }
}
