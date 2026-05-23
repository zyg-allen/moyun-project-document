package com.moyun.community.wallet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.moyun.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cms_recharge")
public class CmsRecharge extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long rechargeId;

    private Long userId;

    private BigDecimal amount;

    private String orderNo;

    private String payType;

    private LocalDateTime payTime;

    private String status;

    private String remark;
}
