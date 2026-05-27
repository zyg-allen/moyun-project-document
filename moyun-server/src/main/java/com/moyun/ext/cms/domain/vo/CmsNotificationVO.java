package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.moyun.core.base.BaseEntity;

import java.time.LocalDateTime;

/**
 * 通知视图对象
 *
 * @author moyun
 */
public class CmsNotificationVO extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 通知ID */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 用户昵称 */
    private String userNickname;

    /** 类型 */
    private String type;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 数据 */
    private String data;

    /** 是否已读 */
    private Boolean isRead;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getUserNickname()
    {
        return userNickname;
    }

    public void setUserNickname(String userNickname)
    {
        this.userNickname = userNickname;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getData()
    {
        return data;
    }

    public void setData(String data)
    {
        this.data = data;
    }

    public Boolean getIsRead()
    {
        return isRead;
    }

    public void setIsRead(Boolean isRead)
    {
        this.isRead = isRead;
    }

    @Override
    public LocalDateTime getCreateTime()
    {
        return createTime;
    }

    @Override
    public void setCreateTime(LocalDateTime createTime)
    {
        this.createTime = createTime;
    }
}
