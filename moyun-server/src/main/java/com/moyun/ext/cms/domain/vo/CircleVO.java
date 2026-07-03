package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 圈子详情 VO
 *
 * @author moyun
 */
@Data
public class CircleVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String description;

    private String cover;

    private Long ownerId;

    private Integer memberCount;

    private Integer postCount;

    private String category;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    // ==================== 圈主信息 ====================

    /** 圈主昵称 */
    private String ownerName;

    /** 圈主头像 */
    private String ownerAvatar;

    // ==================== 当前用户视角 ====================

    /** 当前用户在圈子中的角色：null 未加入 / owner / admin / member */
    private String myRole;

    /** 当前用户是否已加入 */
    private Boolean isJoined;

    /** 成员列表（前 N 个，用于详情页展示） */
    private List<CircleMemberVO> members;
}
