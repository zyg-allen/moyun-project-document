package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 共读打卡记录 VO（列表展示用）
 * <p>
 * 在记录实体字段基础上聚合：作者昵称、作者头像、点赞数、当前用户是否已点赞。
 *
 * @author moyun
 */
@Data
public class BookClubRecordVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    /** 活动ID */
    private Long activityId;

    /** 作者（记录提交者）用户ID */
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

    /** 作者昵称（nickname 为空时回退 username） */
    private String authorName;

    /** 作者头像 */
    private String authorAvatar;

    /** 当前登录用户是否已点赞（未登录返回 false） */
    private Boolean isLiked;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
