package com.moyun.portal.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 门户用户协议同意记录 portal_user_agreement
 *
 * 与门户账号体系共用：注册/登录时记录用户同意的文档类型与版本，
 * 供合规审计证明"用户同意过哪一版协议"。
 *
 * @author moyun
 */
@Data
@TableName("portal_user_agreement")
public class PortalUserAgreement {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 门户用户ID */
    private Long userId;

    /** 文档类型：terms=用户协议，privacy=隐私政策 */
    private String docType;

    /** 协议版本号，如 v1.0 */
    private String version;

    /** 同意时间 */
    private LocalDateTime agreedAt;

    /** 客户端 IP */
    private String ip;

    /** 客户端 UA */
    private String userAgent;
}
