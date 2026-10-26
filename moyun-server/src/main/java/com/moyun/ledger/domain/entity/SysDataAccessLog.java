package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 敏感数据访问日志（sys_data_access_log）
 *
 * <p>后台每一次读取用户个体数据（无论允许还是拒绝）都必须落一条记录，
 * 用于事后追溯"谁、在什么时候、以什么条件、看了谁、依据哪条授权"。</p>
 *
 * <p>只增不改不删；不记录被查看的具体字段值（避免审计表本身变成数据副本）。</p>
 *
 * @author moyun
 */
@Data
@TableName("sys_data_access_log")
public class SysDataAccessLog implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public static final String RESULT_ALLOWED = "allowed";
    public static final String RESULT_DENIED = "denied";

    public static final String ACTION_QUERY = "query";
    public static final String ACTION_EXPORT = "export";

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 操作人 sys_user.user_id */
    private Long operatorId;

    private String operatorName;

    /** 被查看的用户 portal_user.id */
    private Long targetUserId;

    private String bizDomain;

    /** 访问的授权范围 */
    private String scope;

    /** 关联授权 id（拒绝时为空） */
    private Long authId;

    /** query/export */
    private String action;

    /** 查询条件与返回条数（不含字段值） */
    private String queryDesc;

    /** allowed/denied */
    private String result;

    private String clientIp;

    private LocalDateTime accessTime;
}
