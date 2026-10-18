package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 仓库常用配置（key-value，warehouse_id=0 为全局）。 */
@Data
@TableName("ledger_warehouse_config")
public class LedgerWarehouseConfig {
    public static final long GLOBAL_WAREHOUSE = 0L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long warehouseId;
    private String cfgKey;
    private String cfgValue;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}