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
 * 智能体-工具关联实体类
 *
 * <p>对应数据库表 agent_tool_relation，存储智能体与工具的关联关系</p>
 *
 * @author laomao
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("agent_tool_relation")
public class AgentToolRelation {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 智能体ID
     */
    private Long agentId;

    /**
     * 工具ID
     */
    private Long toolId;

    /**
     * 针对该智能体的自定义配置（JSON格式）
     */
    private String customConfig;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
