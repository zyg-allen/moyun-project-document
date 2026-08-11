package com.moyun.ext.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 数据源配置实体
 *
 * <p>继承 {@link AiBaseEntity}，复用 createTime / updateTime / deleted 字段（P3-2 Phase 1）。
 * 原有的 {@code @TableField(fill=...)} 自动填充注解由 AiBaseEntity 统一提供。</p>
 *
 * @author laomao
 */
@Data
@TableName("ai_datasource_config")
public class DataSourceConfig extends AiBaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 数据源名称
     */
    private String name;

    /**
     * 数据源类型: mysql, elasticsearch, mongodb
     */
    private String type;

    /**
     * 主机地址
     */
    private String host;

    /**
     * 端口号
     */
    private Integer port;

    /**
     * 数据库名称
     */
    private String databaseName;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码(加密存储)
     */
    private String password;

    /**
     * 额外连接参数(JSON格式)
     */
    private String connectionParams;

    /**
     * 描述
     */
    private String description;

    /**
     * 是否启用: 0-禁用, 1-启用
     */
    private Boolean enabled;

    /**
     * 健康状态: healthy, unhealthy, unknown
     */
    private String healthStatus;

    /**
     * 最后检查时间
     */
    private LocalDateTime lastCheckTime;

    /**
     * 创建人ID
     */
    private Long createUserId;
}
