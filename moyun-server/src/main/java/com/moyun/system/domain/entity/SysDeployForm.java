package com.moyun.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.moyun.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程部署关联表单对象 sys_deploy_form
 * 
 * @author Tony
 * @date 2021-03-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_deploy_form")
public class SysDeployForm extends BaseEntity {
    
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 表单主键 */
    private Long formId;

    /** 流程定义主键 */
    private String deployId;
}