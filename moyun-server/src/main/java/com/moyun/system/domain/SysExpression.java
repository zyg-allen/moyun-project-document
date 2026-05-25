package com.moyun.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.moyun.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程表达式对象 sys_expression
 *
 * @author ruoyi
 * @date 2022-12-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_expression")
public class SysExpression extends BaseEntity {
    
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 表达式名称 */
    private String name;

    /** 表达式内容 */
    private String expression;

    /** 表达式类型 */
    private String dataType;

    /** 状态 */
    private Integer status;
}