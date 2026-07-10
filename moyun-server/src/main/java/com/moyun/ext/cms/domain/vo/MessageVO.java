package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 私信消息 VO
 *
 * @author moyun
 */
@Data
public class MessageVO {

    /** 消息ID */
    private Long id;

    /** 会话ID */
    private Long sessionId;

    /** 发送者 */
    private Long senderId;

    /** 发送者类型 portal/sys */
    private String senderType;

    /** 接收者 */
    private Long receiverId;

    /** 接收者类型 portal/sys */
    private String receiverType;

    /** 消息内容 */
    private String content;

    /** 消息类型 text/image/file */
    private String msgType;

    /** 是否已读（0未读 1已读） */
    private Integer isRead;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 发送者昵称 */
    private String senderNickname;

    /** 发送者头像 */
    private String senderAvatar;
}
