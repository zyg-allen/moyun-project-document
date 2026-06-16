package com.moyun.system.domain.entity;

import com.moyun.common.annotation.Excel;
import com.moyun.core.base.BaseEntity;
import lombok.Data;

/**
 * 流程任务关联单对象 sys_task_form
 * 
 * @author Tony
 * @date 2021-04-03
 */
@Data
public class SysTaskForm extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 表单主键 */
    @Excel(name = "表单主键")
    private Long formId;

    /** 所属任务 */
    @Excel(name = "所属任务")
    private String taskId;
}
