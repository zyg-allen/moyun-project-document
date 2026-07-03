package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.moyun.core.base.BaseEntity;
import lombok.Data;

/**
 * 任务定义
 *
 * @author moyun
 */
@Data
@TableName("portal_task")
public class PortalTask extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务编码（唯一，用于埋点触发） */
    private String code;

    /** 任务名称 */
    private String name;

    /** 任务描述 */
    private String description;

    /** 任务类型 daily/once/achievement */
    private String taskType;

    /** 完成奖励积分 */
    private Integer rewardPoints;

    /** 目标完成次数 */
    private Integer targetCount;

    /** 任务图标URL */
    private String icon;

    /** 状态 active/inactive */
    private String status;

    public PortalTask() {
    }

    public PortalTask(Long id) {
        this.id = id;
    }
}
