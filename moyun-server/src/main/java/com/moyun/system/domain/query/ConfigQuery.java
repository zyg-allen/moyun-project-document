package com.moyun.system.domain.query;

import com.moyun.core.base.PageDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 参数配置查询对象
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "参数配置查询条件")
public class ConfigQuery extends PageDomain {

    @Schema(description = "搜索关键字")
    private String searchValue;

    /** 参数名称 */
    @Schema(description = "参数名称")
    private String configName;

    /** 参数键名 */
    @Schema(description = "参数键名")
    private String configKey;

    /** 系统内置 */
    @Schema(description = "系统内置")
    private String configType;

    /** 开始时间 */
    @Schema(description = "开始时间")
    private String beginTime;

    /** 结束时间 */
    @Schema(description = "结束时间")
    private String endTime;
}
