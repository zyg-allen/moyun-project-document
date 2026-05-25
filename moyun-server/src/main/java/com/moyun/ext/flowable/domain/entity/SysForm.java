package com.moyun.ext.flowable.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 流程表单配置表 sys_flow_form
 *
 * @author ruoyi
 */
@Data
@TableName("sys_flow_form")
public class SysForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long formId;

    private String formName;

    private String formKey;

    private String formContent;

    private String formType;

    private String status;

    private String createBy;

    private String createTime;

    private String updateBy;

    private String updateTime;

    private String remark;
}
