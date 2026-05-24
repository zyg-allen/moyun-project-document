package com.moyun.community.auth.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 前台用户VO
 *
 * @author moyun
 */
@Data
public class PortalUserVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String username;
    private String nickname;
    private String email;
    private String avatar;
    private String bio;
    private String phone;
    private String wechat;
    private String position;
    private String role;
    private String vipExpireAt;
    private Boolean isPhoneVerified;
    private Boolean isWechatVerified;
    private Boolean twoFactorEnabled;
    private String createdAt;
    private String updatedAt;
    private UserStats stats;

    @Data
    public static class UserStats implements Serializable {
        private static final long serialVersionUID = 1L;
        private Integer articlesCount;
        private Integer followersCount;
        private Integer followingCount;
        private Integer likesCount;
        private Integer viewsCount;
        private Integer bookmarksCount;
        private Integer todayVisitors;
        private Integer totalVisitors;
    }
}
