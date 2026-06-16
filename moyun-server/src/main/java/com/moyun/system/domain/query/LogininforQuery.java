package com.moyun.system.domain.query;

import com.moyun.core.base.page.PageDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 登录日志查询对象
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "登录日志查询条件")
public class LogininforQuery extends PageDomain {

    @Schema(description = "登录IP地址")
    private String ipaddr;

    @Schema(description = "用户账号")
    private String userName;

    @Schema(description = "登录状态（0成功 1失败）")
    private String status;

    @Schema(description = "开始时间")
    private String beginTime;

    @Schema(description = "结束时间")
    private String endTime;
}
