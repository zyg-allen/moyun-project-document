package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.moyun.core.base.BaseEntity;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户任务进度
 *
 * @author moyun
 */
@Data
@TableName("portal_user_task")
public class PortalUserTask extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 任务ID */
    private Long taskId;

    /** 当前进度 */
    private Integer progress;

    /** 是否已完成 0/1 */
    private Integer completed;

    /** 是否已领取奖励 0/1 */
    private Integer claimed;

    /** 完成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedTime;

    public PortalUserTask() {
    }

    public PortalUserTask(Long id) {
        this.id = id;
    }
}
