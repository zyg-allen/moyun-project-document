package com.moyun.portal.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 关注/粉丝列表用户信息 VO
 * <p>
 * 扁平结构，与前端 FollowUserItem 类型对齐。
 * 由 Mapper 通过 JOIN portal_user 查询组装，避免前端 N+1 调用。
 *
 * @author moyun
 */
@Data
public class FollowUserVO {

    /** 关注记录ID（portal_follow.id） */
    private Long id;

    /** 用户ID（粉丝列表中为关注者，关注列表中为被关注者） */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 个人简介 */
    private String bio;

    /** 职位 */
    private String position;

    /** 关注时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
