package com.moyun.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 工作流版本实体
 *
 * @author laomao
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("workflow_version")
public class WorkflowVersion {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 工作流ID */
    @TableField("workflow_id")
    private Long workflowId;
    
    /** 版本号 */
    private Integer version;
    
    /** 版本描述 */
    private String description;
    
    /** 工作流图数据快照 */
    @TableField("graph_data")
    private String graphData;
    
    /** 创建时间 */
    @TableField("create_time")
    private LocalDateTime createTime;
}
