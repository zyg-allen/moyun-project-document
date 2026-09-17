package com.moyun.vip.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 端定义（全局公共：用户/支付/VIP/配置/菜单/统计统一引用）
 *
 * @author moyun
 */
@Data
@TableName("sys_platform")
public class SysPlatform {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 端代码（portal/ledger/admin/personality） */
    private String platformCode;

    /** 端名称 */
    private String platformName;

    /** 端类型：c端/b端 */
    private String platformType;

    /** 描述 */
    private String description;

    /** 绑定域名 */
    private String domain;

    /** 图标 */
    private String icon;

    /** 排序 */
    private Integer sortOrder;

    /** 状态（1启用 0停用） */
    private Integer status;

    private LocalDateTime createTime;
}
