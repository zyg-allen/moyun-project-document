package com.moyun.community.wallet.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.moyun.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cms_wallet")
public class CmsWallet extends BaseEntity {

    @TableId
    private Long userId;

    private BigDecimal balance;

    private BigDecimal frozenBalance;

    private String status;
}
