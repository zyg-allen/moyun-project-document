package com.moyun.ext.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 知识库配置模板实体
 *
 * <p>对应数据库表 knowledge_config_template，存储预定义的配置模板</p>
 *
 * @author laomao
 */
@Data
@TableName("knowledge_config_template")
public class KnowledgeConfigTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 模板描述
     */
    private String templateDesc;

    /**
     * 模板类型：general, technical, legal, medical
     */
    private String templateType;

    /**
     * 配置JSON字符串
     */
    private String configJson;

    /**
     * 是否系统预设模板
     */
    private Boolean isSystem;

    /**
     * 使用次数
     */
    private Integer useCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
