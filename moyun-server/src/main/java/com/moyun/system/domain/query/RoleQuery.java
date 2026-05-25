package com.moyun.system.domain.query;


import com.moyun.core.base.page.PageDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色查询对象
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "角色查询条件")
public class RoleQuery extends PageDomain {

    @Schema(description = "搜索关键字")
    private String searchValue;

    /** 角色名称 */
    @Schema(description = "角色名称")
    private String roleName;

    /** 角色权限 */
    @Schema(description = "角色权限")
    private String roleKey;

    /** 角色状态 */
    @Schema(description = "角色状态")
    private String status;

    /** 角色组 */
    @Schema(description = "角色组")
    private Long[] roleIds;

    /** 开始时间 */
    @Schema(description = "开始时间")
    private String beginTime;

    /** 结束时间 */
    @Schema(description = "结束时间")
    private String endTime;
}
