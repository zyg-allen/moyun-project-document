package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

/**
 * 共读打卡记录表 portal_book_club_record
 *
 * @author moyun
 */
@Data
@TableName("portal_book_club_record")
public class PortalBookClubRecord extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 活动ID */
    private Long activityId;

    /** 用户ID */
    private Long userId;

    /** 第几天 */
    private Integer day;

    /** 打卡内容 */
    private String content;

    /** 图片，逗号分隔 */
    private String images;

    /** 记录类型:reflection-读后感,excerpt-摘抄 */
    private String recordType;

    /** 点赞数 */
    private Long likeCount;
}
