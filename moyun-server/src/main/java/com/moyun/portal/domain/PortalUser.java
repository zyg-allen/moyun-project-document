package com.moyun.portal.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.moyun.common.core.domain.BaseEntity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.LocalDateTime;

/**
 * 门户用户对象 portal_user
 * 
 * @author moyun
 */
@TableName("portal_user")
public class PortalUser extends BaseEntity
{
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

    public PortalUser()
    {

    }

    public PortalUser(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getNickname()
    {
        return nickname;
    }

    public void setNickname(String nickname)
    {
        this.nickname = nickname;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getPhone()
    {
        return phone;
    }

    public void setPhone(String phone)
    {
        this.phone = phone;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getAvatar()
    {
        return avatar;
    }

    public void setAvatar(String avatar)
    {
        this.avatar = avatar;
    }

    public String getBio()
    {
        return bio;
    }

    public void setBio(String bio)
    {
        this.bio = bio;
    }

    public String getPosition()
    {
        return position;
    }

    public void setPosition(String position)
    {
        this.position = position;
    }

    public String getWechat()
    {
        return wechat;
    }

    public void setWechat(String wechat)
    {
        this.wechat = wechat;
    }

    public String getRole()
    {
        return role;
    }

    public void setRole(String role)
    {
        this.role = role;
    }

    public LocalDateTime getVipExpireAt()
    {
        return vipExpireAt;
    }

    public void setVipExpireAt(LocalDateTime vipExpireAt)
    {
        this.vipExpireAt = vipExpireAt;
    }

    public Boolean getIsPhoneVerified()
    {
        return isPhoneVerified;
    }

    public void setIsPhoneVerified(Boolean isPhoneVerified)
    {
        this.isPhoneVerified = isPhoneVerified;
    }

    public Boolean getIsWechatVerified()
    {
        return isWechatVerified;
    }

    public void setIsWechatVerified(Boolean isWechatVerified)
    {
        this.isWechatVerified = isWechatVerified;
    }

    public Boolean getTwoFactorEnabled()
    {
        return twoFactorEnabled;
    }

    public void setTwoFactorEnabled(Boolean twoFactorEnabled)
    {
        this.twoFactorEnabled = twoFactorEnabled;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public String getLoginIp()
    {
        return loginIp;
    }

    public void setLoginIp(String loginIp)
    {
        this.loginIp = loginIp;
    }

    public LocalDateTime getLoginDate()
    {
        return loginDate;
    }

    public void setLoginDate(LocalDateTime loginDate)
    {
        this.loginDate = loginDate;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("userId", getUserId())
            .append("username", getUsername())
            .append("nickname", getNickname())
            .append("email", getEmail())
            .append("phone", getPhone())
            .append("password", getPassword())
            .append("avatar", getAvatar())
            .append("bio", getBio())
            .append("position", getPosition())
            .append("wechat", getWechat())
            .append("role", getRole())
            .append("vipExpireAt", getVipExpireAt())
            .append("isPhoneVerified", getIsPhoneVerified())
            .append("isWechatVerified", getIsWechatVerified())
            .append("twoFactorEnabled", getTwoFactorEnabled())
            .append("status", getStatus())
            .append("delFlag", getDelFlag())
            .append("loginIp", getLoginIp())
            .append("loginDate", getLoginDate())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
