package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.LocalDateTime;

/**
 * 书单-书籍关联表 实体
 *
 * @author moyun
 */
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

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getBookListId()
    {
        return bookListId;
    }

    public void setBookListId(Long bookListId)
    {
        this.bookListId = bookListId;
    }

    public Long getBookId()
    {
        return bookId;
    }

    public void setBookId(Long bookId)
    {
        this.bookId = bookId;
    }

    public Integer getSort()
    {
        return sort;
    }

    public void setSort(Integer sort)
    {
        this.sort = sort;
    }

    public String getNote()
    {
        return note;
    }

    public void setNote(String note)
    {
        this.note = note;
    }

    public LocalDateTime getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime)
    {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("bookListId", getBookListId())
            .append("bookId", getBookId())
            .append("sort", getSort())
            .append("note", getNote())
            .append("createTime", getCreateTime())
            .toString();
    }
}
