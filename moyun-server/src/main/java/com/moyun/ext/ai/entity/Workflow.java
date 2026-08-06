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
 * 工作流定义实体
 *
 * @author laomao
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("workflow")
public class Workflow {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 工作流名称 */
    private String name;
    
    /** 工作流描述 */
    private String description;
    
    /** 工作流图定义(JSON格式，包含nodes和edges) */
    @TableField("graph_data")
    private String graphData;
    
    /** 全局变量定义(JSON格式) */
    private String variables;
    
    /** 状态: draft-草稿, published-已发布, disabled-已禁用 */
    private String status;
    
    /** 版本号 */
    private Integer version;
    
    /** 是否启用 */
    private Boolean enabled;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 更新时间 */
    private LocalDateTime updateTime;
}
