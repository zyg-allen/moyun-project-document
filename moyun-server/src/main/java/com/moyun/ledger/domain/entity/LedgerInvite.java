package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 邀请码：把仓库分享给别人加入。一码多人，有有效期与人数上限。 */
@Data
@TableName("ledger_invite")
public class LedgerInvite {
    public static final String TYPE_WAREHOUSE = "warehouse";
    public static final String ROLE_MEMBER = "member";
    public static final int STATUS_NORMAL = 1;
    public static final int STATUS_REVOKED = 0;

    /** 去掉易混字符（0/O、1/I/L）的 base32 */
    private static final char[] ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ".toCharArray();

    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String type;
    private Long warehouseId;
    private String role;
    private Long createdBy;
    private LocalDateTime expireAt;
    private Integer maxUse;
    private Integer usedCount;
    private Integer status;
    private LocalDateTime createTime;

    public static String randomCode(int len) {
        StringBuilder sb = new StringBuilder(len);
        java.util.Random r = new java.util.Random();
        for (int i = 0; i < len; i++) {
            sb.append(ALPHABET[r.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }
}