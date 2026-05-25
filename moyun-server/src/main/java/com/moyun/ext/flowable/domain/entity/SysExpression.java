package com.moyun.ext.flowable.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 流程表达式配置表 sys_expression
 *
 * @author ruoyi
 */
@Data
@TableName("sys_expression")
public class SysExpression implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long expressionId;

    private String expressionName;

    private String expressionContent;

    private String expressionType;

    private String status;

    private String createBy;

    private String createTime;

    private String updateBy;

    private String updateTime;

    private String remark;
}
