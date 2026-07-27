package com.moyun.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 领域词典实体类
 *
 * <p>对应数据库表 domain_dictionary，存储领域知识词典用于查询扩展</p>
 *
 * @author laomao
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("domain_dictionary")
public class DomainDictionary {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 核心词
    private String keyword;

    // 相关词列表（逗号分隔）
    private String relatedTerms;

    // 分类
    private String category;

    // 词典说明
    private String description;

    // 是否全局词典（全局词典默认对所有智能体生效）
    private Boolean isGlobal;

    // 是否启用
    private Boolean enabled;

    // 优先级
    private Integer priority;

    // 创建时间
    private LocalDateTime createTime;

    // 更新时间
    private LocalDateTime updateTime;
}
