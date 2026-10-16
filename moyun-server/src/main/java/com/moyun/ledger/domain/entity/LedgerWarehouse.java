package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/** 仓库（个人仓/家庭仓/合伙仓）。 */
@Data
@TableName("ledger_warehouse")
public class LedgerWarehouse {
    public static final String TYPE_PERSONAL = "personal";
    public static final String TYPE_FAMILY = "family";
    public static final String TYPE_PARTNER = "partner";
    public static final int STATUS_NORMAL = 1;
    public static final int STATUS_DELETED = 0;

    private Long id;
    private Long userId;
    private String name;
    private String warehouseType;
    private Integer isActive;
    private Integer status;
    private String createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}