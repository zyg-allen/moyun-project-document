package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 共读活动 VO（列表/详情展示用）
 * <p>
 * 在活动实体字段基础上聚合：参与人数、记录数、当前用户是否已加入。
 *
 * @author moyun
 */
@Data
public class BookClubActivityVO implements Serializable {
    private static final long serialVersionUID = 1L;

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

    /** 当前参与人数（活动表冗余字段） */
    private Integer currentParticipants;

    /** 创建者ID */
    private Long createdBy;

    /** 状态:upcoming/ongoing/ended */
    private String status;

    /** 实际参与人数（participant 表聚合） */
    private Long participantsCount;

    /** 打卡记录数（record 表聚合） */
    private Long recordsCount;

    /** 当前登录用户是否已加入该活动（未登录返回 false） */
    private Boolean isJoined;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
