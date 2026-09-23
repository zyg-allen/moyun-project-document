package com.moyun.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 参数配置变更日志表 sys_config_log
 *
 * <p>sys_config 更新时同事务留痕（前后值/操作人/IP），逻辑关联 sys_config，无物理外键。
 *
 * @author moyun
 */
@Data
@TableName("sys_config_log")
public class SysConfigLog {

    /** 日志主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 参数主键（逻辑关联 sys_config.config_id） */
    private Long configId;

    /** 参数键名（冗余快照，便于审计检索） */
    private String configKey;

    /** 变更前键值 */
    private String oldValue;

    /** 变更后键值 */
    private String newValue;

    /** 操作类型（UPDATE） */
    private String operateType;

    /** 操作人员 */
    private String operName;

    /** 操作IP */
    private String operIp;

    /** 创建时间 */
    private LocalDateTime createTime;
}
