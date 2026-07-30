package com.moyun.ext.cms.domain.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

/**
 * 门户用户视图对象
 *
 * <p>后台管理端门户用户列表/详情统一返回 VO。
 * v6.2 扩展：补全 PortalUser 实体中已有的画像字段（学校、公司、地点、网站、GitHub、
 * 性别、生日、认证、VIP、验证状态等），让后台管理员完整掌握客户画像。</p>
 *
 * @author moyun
 */
@Data
public class CmsPortalUserVO extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long id;

    /** 业务主键（前缀usr_，跨表关联） */
    private String businessId;

    /** 关联后台用户ID */
    private Long userId;

    /** 关联后台用户登录名（sys_user.user_name，用于列表展示绑定状态） */
    private String sysUserName;

    /** 关联后台用户昵称（sys_user.nick_name，用于列表展示绑定状态） */
    private String sysNickName;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 头像URL */
    private String avatar;

    /** 个人简介 */
    private String bio;

    /** 职位 */
    private String position;

    /** 微信号 */
    private String wechat;

    /** 性别 */
    private String gender;

    /** 生日 */
    private String birthday;

    /** 所在城市/地点 */
    private String location;

    /** 个人网站 */
    private String website;

    /** GitHub账号 */
    private String github;

    /** 公司 */
    private String company;

    /** 学校 */
    private String school;

    /** 语言偏好 */
    private String language;

    /** 时区 */
    private String timezone;

    /** 角色：user/admin */
    private String role;

    /** 是否认证创作者：0 否/1 是 */
    private Integer isCertifiedCreator;

    /** VIP过期时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime vipExpireAt;

    /** 是否已验证手机号 */
    private Boolean isPhoneVerified;

    /** 是否已验证微信 */
    private Boolean isWechatVerified;

    /** 是否开启两步验证 */
    private Boolean twoFactorEnabled;

    /** 帐号状态（0正常 1停用） */
    private String status;

    /** 最后登录IP */
    private String loginIp;

    /** 最后登录时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime loginDate;
}
