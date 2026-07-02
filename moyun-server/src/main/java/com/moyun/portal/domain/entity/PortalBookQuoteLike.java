package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 金句点赞表 实体
 *
 * @author moyun
 */
@Data
@TableName("portal_book_quote_like")
public class PortalBookQuoteLike
{
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 金句ID */
    private Long quoteId;

    /** 用户ID */
    private Long userId;

    /** 点赞时间 */
    private LocalDateTime createTime;

    public PortalBookQuoteLike()
    {
    }

    public PortalBookQuoteLike(Long id)
    {
        this.id = id;
    }
}
