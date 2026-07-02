package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 书单点赞表 实体
 *
 * @author moyun
 */
@Data
@TableName("portal_book_list_like")
public class PortalBookListLike
{
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 书单ID */
    private Long bookListId;

    /** 用户ID */
    private Long userId;

    /** 点赞时间 */
    private LocalDateTime createTime;

    public PortalBookListLike()
    {
    }

    public PortalBookListLike(Long id)
    {
        this.id = id;
    }
}
