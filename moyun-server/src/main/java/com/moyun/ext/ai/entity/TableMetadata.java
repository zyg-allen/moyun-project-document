package com.moyun.ext.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 表元数据实体
 *
 * @author laomao
 */
@Data
@TableName("table_metadata")
public class TableMetadata {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 数据源ID
     */
    private Long datasourceId;

    /**
     * 表名
     */
    private String tableName;

    /**
     * 表注释
     */
    private String tableComment;

    /**
     * 表结构(JSON格式)
     */
    private String tableSchema;

    /**
     * 字段数量
     */
    private Integer columnCount;

    /**
     * 行数(估算)
     */
    private Long rowCount;

    /**
     * 数据大小(字节)
     */
    private Long dataSize;

    /**
     * 是否有主键
     */
    private Boolean hasPrimaryKey;

    /**
     * 是否有时间字段
     */
    private Boolean hasTimeField;

    /**
     * 时间字段名
     */
    private String timeFieldName;

    /**
     * 数值型字段列表(JSON)
     */
    private String numericFields;

    /**
     * 类别型字段列表(JSON)
     */
    private String categoryFields;

    /**
     * 索引字段列表(JSON)
     */
    private String indexedFields;

    /**
     * 最后同步时间
     */
    private LocalDateTime lastSyncTime;

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
}
