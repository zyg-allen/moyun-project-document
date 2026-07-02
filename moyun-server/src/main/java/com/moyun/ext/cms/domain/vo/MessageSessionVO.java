package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 私信会话 VO
 *
 * <p>包含会话信息、对方用户信息、最后消息预览及当前用户未读数。</p>
 *
 * @author moyun
 */
@Data
public class MessageSessionVO {

    /** 会话ID */
    private Long id;

    /** 对方用户信息 */
    private PeerUser peerUser;

    /** 最后消息内容预览 */
    private String lastMessageContent;

    /** 最后消息时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastMessageTime;

    /** 当前用户未读数 */
    private Integer unreadCount;

    /**
     * 对方用户信息
     */
    @Data
    public static class PeerUser {
        /** 对方用户ID */
        private Long id;
        /** 对方昵称 */
        private String nickname;
        /** 对方头像 */
        private String avatar;
    }
}
