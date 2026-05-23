package com.moyun.community.auth.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserVo implements Serializable {

    private Long userId;

    private String username;

    private String nickname;

    private String avatar;

    private String email;

    private String phonenumber;

    private String gender;

    private String bio;

    private Integer points;

    private Integer level;

    private java.math.BigDecimal inkBalance;

    private Integer articleCount;

    private Integer followerCount;

    private Integer followingCount;

    private Long likeCount;

    private Integer isAuthor;

    private Integer authorLevel;
}
