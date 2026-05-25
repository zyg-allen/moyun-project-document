package com.moyun.ext.flowable.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 流程任务表单配置表 sys_task_form
 *
 * @author ruoyi
 */
@Data
@TableName("sys_task_form")
public class SysTaskForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long taskFormId;

    private String processDefinitionId;

    private String processDefinitionKey;

    private String taskName;

    private String formId;

    private String formType;

    private String status;

    private String createBy;

    private String createTime;
}
