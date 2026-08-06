package com.moyun.ext.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表信息VO
 * 
 * @author laomao
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableInfoVO {
    
    /**
     * 表名
     */
    private String tableName;
    
    /**
     * 表注释
     */
    private String tableComment;
    
    /**
     * 数据行数
     */
    private Long rowCount;
    
    /**
     * 表大小（字节）
     */
    private Long dataLength;
    
    /**
     * 表大小（格式化，如 1.2 MB）
     */
    private String dataSizeFormatted;
    
    /**
     * 创建时间
     */
    private String createTime;
    
    /**
     * 更新时间
     */
    private String updateTime;
}
