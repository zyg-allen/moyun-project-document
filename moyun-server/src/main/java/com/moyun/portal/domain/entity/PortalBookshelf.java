package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

import java.time.LocalDateTime;

/**
 * 用户书架（收藏书籍）实体 portal_bookshelf
 *
 * @author moyun
 */
@Data
@TableName("portal_bookshelf")
public class PortalBookshelf extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 书籍ID */
    private Long bookId;

    /** 最后阅读章节ID（冗余，用于续读） */
    private Long lastChapterId;

    /** 最后阅读章节序号 */
    private Integer lastChapterNo;

    /** 排序（用户自定义书架顺序，越大越靠前） */
    private Integer sort;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    public PortalBookshelf() {
    }

    public PortalBookshelf(Long id) {
        this.id = id;
    }
}
