package com.moyun.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.moyun.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程表单对象 sys_form
 * 
 * @author Tony
 * @date 2021-03-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_form")
public class SysForm extends BaseEntity {
    
    private static final long serialVersionUID = 1L;

    /** 表单主键 */
    @TableId(type = IdType.AUTO)
    private Long formId;

    /** 表单名称 */
    private String formName;

    /** 表单内容 */
    private String formContent;
}