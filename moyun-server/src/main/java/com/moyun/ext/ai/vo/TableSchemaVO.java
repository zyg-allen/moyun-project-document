package com.moyun.ext.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 表结构VO
 *
 * @author laomao
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableSchemaVO {

    /**
     * 表名
     */
    private String tableName;

    /**
     * 表注释
     */
    private String tableComment;

    /**
     * 列信息
     */
    private List<ColumnSchema> columns;

    /**
     * 主键字段
     */
    private List<String> primaryKeys;

    /**
     * 索引字段
     */
    private List<String> indexedFields;

    /**
     * 行数
     */
    private Long rowCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnSchema {
        /**
         * 字段名
         */
        private String columnName;

        /**
         * 数据类型
         */
        private String dataType;

        /**
         * 字段注释
         */
        private String comment;
        
        /**
         * 字段注释(别名，兼容前端)
         */
        private String columnComment;

        /**
         * 是否可为空
         */
        private Boolean nullable;

        /**
         * 是否主键
         */
        private Boolean primaryKey;
        
        /**
         * 列键类型: PRI(主键), UNI(唯一键), MUL(普通索引)
         */
        private String columnKey;
        
        /**
         * 默认值
         */
        private String columnDefault;
        
        /**
         * 额外信息: auto_increment等
         */
        private String extra;

        /**
         * 字段类型分类: numeric, datetime, category, text
         */
        private String fieldType;
    }
}
