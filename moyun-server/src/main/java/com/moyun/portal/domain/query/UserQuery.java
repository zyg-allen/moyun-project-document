package com.moyun.portal.domain.query;

import com.moyun.core.base.page.PageDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户查询对象
 *
 * @author moyun
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户查询对象")
public class UserQuery extends PageDomain {

    /**
     * 用户名（模糊查询）
     */
    @Schema(description = "用户名", example = "john")
    private String username;

    /**
     * 昵称（模糊查询）
     */
    @Schema(description = "昵称", example = "John")
    private String nickname;

    /**
     * 邮箱（模糊查询）
     */
    @Schema(description = "邮箱", example = "john@example.com")
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
     * 帐号状态
     */
    @Schema(description = "帐号状态", example = "0")
    private String status;

    /**
     * 是否查询VIP用户
     */
    @Schema(description = "是否查询VIP用户", example = "false")
    private Boolean vipOnly;

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
