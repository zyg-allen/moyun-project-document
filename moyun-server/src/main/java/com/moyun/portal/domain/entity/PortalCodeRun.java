package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.moyun.core.base.BaseEntity;
import lombok.Data;

/**
 * 代码运行记录 portal_code_run
 * <p>
 * 沙箱执行器执行用户代码后落库的运行历史。
 * 业务字段 create_time 复用 BaseEntity.createTime（驼峰映射一致，无需重声明），
 * 其余 BaseEntity 公共字段 createBy/updateBy/updateTime/remark 在本表不存在，需排除映射。
 *
 * @author moyun
 */
@Data
@TableName("portal_code_run")
public class PortalCodeRun extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 运行者用户ID */
    private Long userId;

    /** 编程语言 java/python/javascript */
    private String language;

    /** 用户提交的源代码 */
    private String code;

    /** 标准输入内容 */
    private String stdin;

    /** 标准输出（截断至 1MB） */
    private String output;

    /** 错误输出 / 编译错误信息 */
    private String errorMsg;

    /** 运行状态 running/success/failed/timeout */
    private String status;

    /** 运行耗时（毫秒） */
    private Integer runtimeMs;

    /** 内存占用（KB，粗略估算） */
    private Integer memKb;

    // BaseEntity 公共字段对应列在本表不存在，排除 MyBatis-Plus 映射，避免 SELECT/INSERT 报未知列
    // createTime 复用父类字段（对应 create_time 列），不重声明
    @TableField(exist = false)
    private String createBy;
    @TableField(exist = false)
    private String updateBy;
    @TableField(exist = false)
    private java.time.LocalDateTime updateTime;
    @TableField(exist = false)
    private String remark;

    // 覆盖 BaseEntity 的 delFlag：本表无 del_flag 列（迁移脚本排除），保持物理删除（toggle/流水语义）
    @TableField(exist = false)
    private String delFlag;
}
