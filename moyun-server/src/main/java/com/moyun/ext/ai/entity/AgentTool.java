package com.moyun.ext.ai.entity;

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
 * 智能体工具实体类
 *
 * <p>对应数据库表 agent_tool，存储工具定义信息</p>
 *
 * @author laomao
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("agent_tool")
public class AgentTool {
    
    /**
     * 工具ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 工具标识（英文）
     */
    private String name;

    /**
     * 显示名称（中文）
     */
    private String displayName;

    /**
     * 工具描述（给LLM理解用）
     */
    private String description;

    /**
     * 工具分类：general/information/utility/action/data
     */
    private String category;

    /**
     * 工具类型：builtin/http/database
     */
    private String toolType;

    /**
     * 图标（FontAwesome）
     */
    private String icon;

    /**
     * 工具配置（JSON格式，API地址、认证信息等）
     */
    private String config;

    /**
     * 参数定义（JSON Schema格式）
     */
    private String parameters;

    /**
     * 超时时间（秒）
     */
    private Integer timeoutSeconds;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 是否系统内置
     */
    @TableField("is_system")
    private Boolean isSystem;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
