package com.moyun.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 数据源配置实体
 *
 * @author laomao
 */
@Data
@TableName("datasource_config")
public class DataSourceConfig {

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

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 删除标记: 0-未删除, 1-已删除
     */
    @TableLogic
    private Boolean deleted;
}
