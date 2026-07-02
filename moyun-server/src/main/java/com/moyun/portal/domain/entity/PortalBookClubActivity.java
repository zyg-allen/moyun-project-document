package com.moyun.portal.domain.entity;

import java.time.LocalDate;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

/**
 * 共读活动表 portal_book_club_activity
 *
 * @author moyun
 */
@Data
@TableName("portal_book_club_activity")
public class PortalBookClubActivity extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 活动标题 */
    private String title;

    /** 书籍ID */
    private Long bookId;

    /** 活动描述 */
    private String description;

    /** 活动封面 */
    private String cover;

    /** 开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /** 结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /** 最大参与人数 */
    private Integer maxParticipants;

    /** 当前参与人数 */
    private Integer currentParticipants;

    /** 创建者ID */
    private Long createdBy;

    /** 状态:upcoming-未开始,ongoing-进行中,ended-已结束 */
    private String status;
}
