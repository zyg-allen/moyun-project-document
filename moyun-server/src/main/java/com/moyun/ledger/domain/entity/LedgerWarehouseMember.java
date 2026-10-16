package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/** 仓库成员（家庭仓/合伙仓共享）。 */
@Data
@TableName("ledger_warehouse_member")
public class LedgerWarehouseMember {
    public static final int STATUS_NORMAL = 1;
    public static final int STATUS_DELETED = 0;

    private Long id;
    private Long warehouseId;
    private Long userId;
    private String role;
    private Long invitedBy;
    private LocalDateTime joinedAt;
    private Integer isActive;
    private Integer status;
    private LocalDateTime createTime;
}