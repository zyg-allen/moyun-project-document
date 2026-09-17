package com.moyun.vip.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * VIP 接口注册表（@VipOnly 启动扫描 upsert 生成，运营可编辑描述/启停）
 *
 * @author moyun
 */
@Data
@TableName("vip_api_registry")
public class VipApiRegistry {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接口路径（URL pattern） */
    private String apiPath;

    /** HTTP 方法 */
    private String httpMethod;

    /** Controller 类全名 */
    private String controllerClass;

    /** 方法名 */
    private String methodName;

    /** 端代码 */
    private String platformCode;

    /** 权益代码 */
    private String benefitCode;

    /** 是否消耗次数（1消耗 0仅校验） */
    private Integer consume;

    /** 校验失败提示 */
    private String message;

    /** 接口描述（运营填写） */
    private String apiDesc;

    /** 是否启用校验（0=后台禁用该接口校验，直接放行） */
    private Integer enabled;

    /** 最近扫描时间 */
    private LocalDateTime scanTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
