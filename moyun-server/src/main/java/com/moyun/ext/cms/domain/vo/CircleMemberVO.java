package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 圈子成员信息
 *
 * @author moyun
 */
@Data
public class CircleMemberVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long circleId;

    private Long userId;

    /** 角色 owner/admin/member */
    private String role;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime joinedTime;

    /** 用户昵称 */
    private String nickname;

    /** 用户头像 */
    private String avatar;
}
