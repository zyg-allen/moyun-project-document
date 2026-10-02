package com.moyun.portal.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 门户**公开**用户资料 VO（他人主页可见字段）。
 *
 * <p><b>为什么必须有它</b>：{@code GET /portal/user/{id}} 原先直接返回 {@code PortalUser} **实体全字段**，
 * 未登录也能拉到目标用户的 {@code email / phone / wechat / loginIp / loginDate / maritalStatus /
 * hasMortgage / hasSideIncome / incomeTypes} 等隐私与画像字段（Service 已清密码，但其余字段照发）。
 * 本 VO 采用**白名单**：只保留主页展示所需字段。</p>
 *
 * <p><b>联系方式一律不下发</b>：公开主页没有消费方需要 {@code email/phone/wechat}，
 * 故最小权限地直接省略（历史上本 VO 的死代码版本含有这些字段，已删除）；
 * 若将来要做"联系作者"，必须单独开接口并按 {@code privacyEmail/privacyPhone} 二次校验。</p>
 *
 * <p>同样不下发：{@code role}（角色推测）、{@code status}/{@code loginIp}/{@code loginDate}
 * （账号安全信息）、{@code vipExpireAt}/{@code isPhoneVerified}/{@code isWechatVerified}/
 * {@code twoFactorEnabled}（账户与安全状态）。</p>
 *
 * @author moyun
 */
@Data
@Schema(description = "门户公开用户资料VO（他人主页可见）")
public class UserProfileVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID", example = "1")
    private Long id;

    @Schema(description = "用户名（主页展示与 SEO 标题使用）", example = "john_doe")
    private String username;

    @Schema(description = "昵称", example = "John Doe")
    private String nickname;

    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    @Schema(description = "个人简介", example = "热爱技术，喜欢分享")
    private String bio;

    @Schema(description = "职位", example = "Java Developer")
    private String position;

    @Schema(description = "公司")
    private String company;

    @Schema(description = "学校")
    private String school;

    @Schema(description = "所在地")
    private String location;

    @Schema(description = "个人网站")
    private String website;

    @Schema(description = "GitHub")
    private String github;

    @Schema(description = "身份标签")
    private String identityTag;

    @Schema(description = "性别")
    private String gender;

    @Schema(description = "是否认证创作者（主页认证徽章）")
    private Boolean certifiedCreator;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "注册时间（主页展示加入时间）")
    private LocalDateTime createTime;
}
