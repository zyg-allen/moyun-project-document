package com.moyun.portal.domain.entity;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

/**
 * 书单表 实体
 *
 * @author moyun
 */
@Data
@TableName("portal_book_list")
public class PortalBookList extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String description;

    private String cover;

    @NotNull(message = "创建者ID不能为空")
    private Long userId;

    private Long categoryId;

    private Boolean isPublic;

    private Integer bookCount;

    private Long viewCount;

    private Long likeCount;

    private String status;

    /** 是否精选（首页展示用） */
    private Boolean isFeatured;

    /** 访问级别: free=免费公开, vip=会员专享 */
    private String accessLevel;

    /** 标签（逗号分隔） */
    private String tags;

    public PortalBookList()
    {
    }

    public PortalBookList(Long id)
    {
        this.id = id;
    }
}
