package com.moyun.ext.flowable.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 流程监听器配置表 sys_flow_listener
 *
 * @author ruoyi
 */
@Data
@TableName("sys_flow_listener")
public class SysListener implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long listenerId;

    private String listenerName;

    private String listenerType;

    private String eventType;

    private String listenerValue;

    private String status;

    private String createBy;

    private String createTime;

    private String updateBy;

    private String updateTime;

    private String remark;
}
