package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 售卖渠道（抖音/淘宝/闲鱼…）。user_id=0 为系统预设。 */
@Data
@TableName("ledger_sale_channel")
public class LedgerSaleChannel {
    public static final long SYSTEM_USER_ID = 0L;
    public static final int STATUS_ENABLED = 1;
    public static final int STATUS_DISABLED = 0;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private String name;
    private String icon;
    private Integer sortOrder;
    private Integer isSystem;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
