package com.moyun.ext.cms.domain.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

/**
 * 通知视图对象
 *
 * @author moyun
 */
@Data
public class CmsNotificationVO extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 通知ID */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 用户昵称 */
    private String userNickname;

    /** 类型 */
    private String type;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 数据 */
    private String data;

    /** 是否已读 */
    private Boolean isRead;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 备注 */
    private String remark;
}
