package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 书单-书籍关联表 实体
 *
 * @author moyun
 */
@Data
@TableName("portal_book_list_item")
public class PortalBookListItem
{
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 书单ID */
    private Long bookListId;

    /** 书籍ID */
    private Long bookId;

    /** 排序 */
    private Integer sort;

    /** 添加说明/推荐语 */
    private String note;

    /** 创建时间 */
    private LocalDateTime createTime;

    public PortalBookListItem()
    {
    }
}
