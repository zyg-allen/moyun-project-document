package com.moyun.community.wallet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.moyun.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cms_transaction")
public class CmsTransaction extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long transactionId;

    private Long userId;

    private String type;

    private BigDecimal amount;

    private BigDecimal balance;

    private BigDecimal balanceBefore;

    private BigDecimal balanceAfter;

    private String sourceType;

    private Long sourceId;

    private Long relatedId;

    private String status;

    private String remark;
}
