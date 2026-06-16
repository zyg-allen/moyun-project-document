package com.moyun.system.domain.query;

import com.moyun.core.base.page.PageDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 操作日志查询对象
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "操作日志查询条件")
public class OperLogQuery extends PageDomain {

    @Schema(description = "模块标题")
    private String title;

    @Schema(description = "业务类型（0其它 1新增 2修改 3删除）")
    private Integer businessType;

    @Schema(description = "操作状态（0正常 1异常）")
    private Integer status;

    @Schema(description = "操作人员")
    private String operName;

    @Schema(description = "主机地址")
    private String operIp;

    @Schema(description = "开始时间")
    private String beginTime;

    @Schema(description = "结束时间")
    private String endTime;

    /** 业务类型数组（批量查询） */
    private Integer[] businessTypes;
}
