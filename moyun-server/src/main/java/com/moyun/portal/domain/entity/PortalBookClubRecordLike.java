package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

/**
 * 打卡点赞表 portal_book_club_record_like
 *
 * @author moyun
 */
@Data
@TableName("portal_book_club_record_like")
public class PortalBookClubRecordLike extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 打卡记录ID */
    private Long recordId;

    /** 用户ID */
    private Long userId;
}
