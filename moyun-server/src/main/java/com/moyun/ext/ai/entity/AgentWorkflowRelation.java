package com.moyun.ext.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 智能体-工作流关联实体
 * 
 * <p>将工作流绑定到智能体，使智能体可以调用工作流</p>
 *
 * @author laomao
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("agent_workflow_relation")
public class AgentWorkflowRelation {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 智能体ID */
    private Long agentId;
    
    /** 工作流ID */
    private Long workflowId;
    
    /** 是否启用 */
    private Boolean enabled;
    
    /** 排序 */
    private Integer sortOrder;
    
    /** 创建时间 */
    private LocalDateTime createTime;
}
