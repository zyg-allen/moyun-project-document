package com.moyun.community.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sys_user_profile")
public class UserProfile {

    @TableId(type = IdType.INPUT)
    private Long userId;

    private String nickname;

    private String avatar;

    private String bio;

    private String gender;

    private LocalDateTime birthday;

    private String location;

    private String website;

    private String socialLinks;

    private Integer points;

    private Integer level;

    private BigDecimal inkBalance;

    private Long totalReadTime;

    private Long totalWordCount;

    private Integer articleCount;

    private Integer followerCount;

    private Integer followingCount;

    private Long likeCount;

    private Long articleViewCount;

    private Integer isAuthor;

    private Integer authorLevel;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}