package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 进货渠道（拼多多 / 闲鱼 / 批发市场…）。
 * user_id = 0 为系统预设；用户可自建，长按可删。
 *
 * 与 source_type 互补：
 *   source_type    来源类型（固定枚举，决定成本怎么算）
 *   sourceChannel  进货渠道（自由维护，记录从哪来）
 */
@Data
@TableName("ledger_source_channel")
public class LedgerSourceChannel {
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
