package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("portal_book_quote")
public class PortalBookQuote
{
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long bookId;

    private String content;

    private String page;

    private String chapter;

    private Long likeCount;

    private Boolean isPublic;

    /** 是否精选（首页展示用） */
    private Boolean isFeatured;

    /** 章节标题/位置描述 */
    private String location;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public PortalBookQuote()
    {
    }

    public PortalBookQuote(Long id)
    {
        this.id = id;
    }
}
