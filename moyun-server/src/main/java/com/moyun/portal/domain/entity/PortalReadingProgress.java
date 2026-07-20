package com.moyun.portal.domain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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

    // -------------------------------------------------------
    // v1.0 第二阶段新增：章节级进度记忆（由 42 号 SQL 扩展）
    // -------------------------------------------------------

    /** 当前阅读章节ID */
    private Long currentChapterId;

    /** 当前章节序号 */
    private Integer currentChapterNo;

    /** 章节内滚动偏移（像素，用于续读恢复） */
    private Integer chapterOffset;

    /** 最后阅读时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastReadTime;

    /** 累计阅读时长（毫秒） */
    private Long readingDurationMs;

    /**
     * v1.0 阅读闭环：前端上报章节完成标记
     * <p>非持久化字段（@TableField(exist = false)），仅作为完成事件触发信号在请求体中传递。</p>
     * <p>触发逻辑：前端检测到用户阅读到章节底部时上报 chapterFinished=true，
     * 若当前章节为最后一章，后端将整书 status 置为 finished 并触发成长事件 + Feed 动态。</p>
     */
    @TableField(exist = false)
    private Boolean chapterFinished;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    public PortalReadingProgress()
    {
    }
}
