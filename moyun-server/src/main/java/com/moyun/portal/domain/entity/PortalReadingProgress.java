package com.moyun.portal.domain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

/**
 * 阅读进度表 实体
 *
 * @author moyun
 */
@Data
@TableName("portal_reading_progress")
public class PortalReadingProgress extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 书籍ID */
    private Long bookId;

    /** 阅读状态: want_to_read, reading, finished */
    private String status;

    /** 阅读进度百分比 */
    private Integer progress;

    /** 已读页数 */
    private Integer pagesRead;

    /** 开始阅读日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /** 完成日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate finishDate;

    /** 阅读笔记/读后感 */
    private String note;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    public PortalReadingProgress()
    {
    }
}
