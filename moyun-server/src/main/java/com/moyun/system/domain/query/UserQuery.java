package com.moyun.system.domain.query;

import com.moyun.core.base.PageDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户查询对象
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户查询条件")
public class UserQuery extends PageDomain {

    @Schema(description = "搜索关键字")
    private String searchValue;

    /** 用户账号 */
    @Schema(description = "用户账号")
    private String userName;

    /** 用户昵称 */
    @Schema(description = "用户昵称")
    private String nickName;

    /** 部门ID */
    @Schema(description = "部门ID")
    private Long deptId;

    /** 手机号码 */
    @Schema(description = "手机号码")
    private String phonenumber;

    /** 状态 */
    @Schema(description = "状态")
    private String status;

    /** 角色ID */
    @Schema(description = "角色ID")
    private Long roleId;

    /** 开始时间 */
    @Schema(description = "开始时间")
    private String beginTime;

    /** 结束时间 */
    @Schema(description = "结束时间")
    private String endTime;
}
