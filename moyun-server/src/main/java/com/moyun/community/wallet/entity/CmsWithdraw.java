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
@TableName("cms_withdraw")
public class CmsWithdraw extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long withdrawId;

    private Long userId;

    private BigDecimal amount;

    private BigDecimal fee;

    private BigDecimal actualAmount;

    private String orderNo;

    private String bankName;

    private String bankCard;

    private String status;

    private String remark;
}
