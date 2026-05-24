package com.moyun.community.content.model.vo;

import lombok.Data;
import java.io.Serializable;

/**
 * 前台关注 VO
 *
 * @author moyun
 */
@Data
public class PortalFollowVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private Long targetUserId;
    private String userAvatar;
    private String userNickname;
    private String userBio;
    private String createdAt;
    private Boolean isFollowing;
}
