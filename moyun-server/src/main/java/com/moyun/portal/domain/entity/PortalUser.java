package com.moyun.portal.domain.entity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

/**
 * 门户用户对象 portal_user
 *
 * @author moyun
 */
@TableName("portal_user")
@Data
public class PortalUser extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联后台用户ID */
    private Long userId;

    /** 用户名 */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 0, max = 50, message = "用户名长度不能超过50个字符")
    private String username;

    /** 昵称 */
    @Size(min = 0, max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

    /** 邮箱 */
    @Email(message = "邮箱格式不正确")
    @Size(min = 0, max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    /** 手机号 */
    @Size(min = 0, max = 20, message = "手机号长度不能超过20个字符")
    private String phone;

    /** 密码 */
    private String password;

    /** 头像URL */
    @Size(min = 0, max = 500, message = "头像URL长度不能超过500个字符")
    private String avatar;

    /** 个人简介 */
    @Size(min = 0, max = 500, message = "个人简介长度不能超过500个字符")
    private String bio;

    /** 职位 */
    @Size(min = 0, max = 100, message = "职位长度不能超过100个字符")
    private String position;

    /** 微信号 */
    @Size(min = 0, max = 100, message = "微信号长度不能超过100个字符")
    private String wechat;

    /** 性别 */
    @Size(min = 0, max = 20, message = "性别长度不能超过20个字符")
    private String gender;

    /** 生日 */
    @Size(min = 0, max = 20, message = "生日长度不能超过20个字符")
    private String birthday;

    /** 所在城市/地点 */
    @Size(min = 0, max = 100, message = "地点长度不能超过100个字符")
    private String location;

    /** 个人网站 */
    @Size(min = 0, max = 200, message = "网站长度不能超过200个字符")
    private String website;

    /** GitHub账号 */
    @Size(min = 0, max = 100, message = "GitHub长度不能超过100个字符")
    private String github;

    /** 公司 */
    @Size(min = 0, max = 200, message = "公司长度不能超过200个字符")
    private String company;

    /** 学校 */
    @Size(min = 0, max = 200, message = "学校长度不能超过200个字符")
    private String school;

    /** 语言偏好 */
    @Size(min = 0, max = 20, message = "语言长度不能超过20个字符")
    private String language;

    /** 时区 */
    @Size(min = 0, max = 50, message = "时区长度不能超过50个字符")
    private String timezone;

    /** 是否接收点赞通知 */
    private Boolean notifyLike;

    /** 是否接收评论通知 */
    private Boolean notifyComment;

    /** 是否接收关注通知 */
    private Boolean notifyFollow;

    /** 是否接收系统通知 */
    private Boolean notifySystem;

    /** 是否允许被关注 */
    private Boolean privacyFollow;

    /** 是否公开收藏夹 */
    private Boolean privacyBookmark;

    /** 是否公开邮箱 */
    private Boolean privacyEmail;

    /** 是否公开手机号 */
    private Boolean privacyPhone;

    /** 角色：user/admin */
    @Size(min = 0, max = 20, message = "角色长度不能超过20个字符")
    private String role;

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

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    /** 最后登录IP */
    private String loginIp;

    /** 最后登录时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime loginDate;

    public PortalUser() {

    }

    public PortalUser(Long id) {
        this.id = id;
    }
}
