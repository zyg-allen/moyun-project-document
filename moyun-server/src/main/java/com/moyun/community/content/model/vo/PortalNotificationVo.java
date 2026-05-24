package com.moyun.community.content.model.vo;

import lombok.Data;
import java.io.Serializable;

/**
 * 前台通知 VO
 *
 * @author moyun
 */
@Data
public class PortalNotificationVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String type;
    private String title;
    private String content;
    private Boolean isRead;
    private String relatedId;
    private String createdAt;
}
