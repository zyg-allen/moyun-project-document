package com.moyun.ledger.domain.vo;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

/**
 * 存钱计划列表 VO：计划卡片 + 汇总，前端字段与旧 Map 完全兼容。
 */
@Data
public class SavingPlanListVO {

    private List<Item> plans;
    private BigDecimal totalTarget;
    private BigDecimal totalSaved;
    private BigDecimal totalRemaining;

    @Data
    public static class Item {
        private Long id;
        private String name;
        private String method;
        private BigDecimal targetAmount;
        private BigDecimal currentAmount;
        private Integer status;
    }
}
