package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 商品可售渠道（1:N）。 */
@Data
@TableName("ledger_product_channel")
public class LedgerProductChannel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private Long channelId;
    private LocalDateTime createTime;
}