package com.moyun.system.domain.vo;

import java.time.LocalDateTime;

import com.moyun.system.domain.entity.SysNotification;

import lombok.Data;

/**
 * 通知列表 VO：门户消息中心使用，去掉 userId/userType 等内部字段。
 */
@Data
public class NotificationVO {

    private Long id;
    private String type;
    private String title;
    private String content;
    private String data;
    private String scope;
    private String noticeType;
    private String status;
    private Boolean isRead;
    private LocalDateTime createTime;

    public static NotificationVO from(SysNotification n) {
        if (n == null) {
            return null;
        }
        NotificationVO v = new NotificationVO();
        v.setId(n.getId());
        v.setType(n.getType());
        v.setTitle(n.getTitle());
        v.setContent(n.getContent());
        v.setData(n.getData());
        v.setScope(n.getScope());
        v.setNoticeType(n.getNoticeType());
        v.setStatus(n.getStatus());
        v.setIsRead(n.getIsRead());
        v.setCreateTime(n.getCreateTime());
        return v;
    }
}
