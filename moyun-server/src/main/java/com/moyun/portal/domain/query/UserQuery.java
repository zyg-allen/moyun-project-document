package com.moyun.portal.domain.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import com.moyun.core.base.page.PageDomain;

/**
 * 门户用户查询对象
 *
 * @author moyun
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "门户用户查询对象")
public class UserQuery extends PageDomain {

    /**
     * 用户名（模糊查询）
     */
    @Schema(description = "用户名", example = "test")
    private String username;

    /**
     * 昵称（模糊查询）
     */
    @Schema(description = "昵称", example = "小明")
    private String nickname;

    /**
     * 邮箱（模糊查询）
     */
    @Schema(description = "邮箱", example = "test@example.com")
    private String email;

    /**
     * 手机号
     */
    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    /**
     * 角色
     */
    @Schema(description = "角色", example = "user")
    private String role;

    /**
     * 状态
     */
    @Schema(description = "状态", example = "0")
    private String status;

    /**
     * 删除标志
     */
    @Schema(description = "删除标志", example = "0")
    private String delFlag;

    /**
     * 开始时间
     */
    @Schema(description = "开始时间", example = "2024-01-01")
    private String startTime;

    /**
     * 结束时间
     */
    @Schema(description = "结束时间", example = "2024-12-31")
    private String endTime;
}
