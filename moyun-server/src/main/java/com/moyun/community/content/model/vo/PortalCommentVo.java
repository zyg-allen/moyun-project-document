package com.moyun.community.content.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 前台评论VO
 *
 * @author moyun
 */
@Data
public class PortalCommentVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String articleId;
    private String parentId;
    private String content;
    private String contentHtml;
    private String userId;
    private String userNickname;
    private String userAvatar;
    private String replyToUserId;
    private String replyToUserNickname;
    private Integer likes;
    private Boolean isLiked;
    private String createdAt;
    private String updatedAt;
    private List<PortalCommentVo> replies;
}
