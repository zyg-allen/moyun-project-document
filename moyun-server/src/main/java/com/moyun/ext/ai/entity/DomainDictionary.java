package com.moyun.ext.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 领域词典实体类
 *
 * <p>对应数据库表 domain_dictionary，存储领域知识词典用于查询扩展</p>
 *
 * <p>继承 {@link AiBaseEntity}，复用 createTime / updateTime / deleted 字段（P3-2 Phase 1）。</p>
 *
 * @author laomao
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("ai_domain_dictionary")
public class DomainDictionary extends AiBaseEntity {

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
}
