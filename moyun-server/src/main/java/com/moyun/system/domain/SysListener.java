package com.moyun.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.moyun.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程监听对象 sys_listener
 * 
 * @author Tony
 * @date 2022-12-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_listener")
public class SysListener extends BaseEntity {
    
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 名称 */
    private String name;

    /** 监听类型 */
    private String type;

    /** 事件类型 */
    private String eventType;

    /** 值类型 */
    private String valueType;

    /** 执行内容 */
    private String value;

    /** 状态 */
    private Integer status;
}