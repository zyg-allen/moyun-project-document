package com.moyun.ext.flowable.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 流程部署表单配置表 sys_deploy_form
 *
 * @author ruoyi
 */
@Data
@TableName("sys_deploy_form")
public class SysDeployForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long deployFormId;

    private String deploymentId;

    private String formId;

    private String formType;

    private String status;

    private String createBy;

    private String createTime;
}
