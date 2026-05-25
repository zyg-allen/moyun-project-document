package com.moyun.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 参数配置视图对象
 *
 * @author ruoyi
 */
@Data
@Schema(description = "参数配置VO")
public class ConfigVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 参数主键 */
    @Schema(description = "参数主键")
    private Long configId;

    /** 参数名称 */
    @Schema(description = "参数名称")
    private String configName;

    /** 参数键名 */
    @Schema(description = "参数键名")
    private String configKey;

    /** 参数键值 */
    @Schema(description = "参数键值")
    private String configValue;

    /** 系统内置（Y是 N否） */
    @Schema(description = "系统内置")
    private String configType;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;
}
