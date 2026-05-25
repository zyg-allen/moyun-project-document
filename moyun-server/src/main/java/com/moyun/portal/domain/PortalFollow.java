package com.moyun.portal.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.moyun.common.core.domain.BaseEntity;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.LocalDateTime;

@TableName("portal_follow")
public class PortalFollow extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotNull(message = "关注者ID不能为空")
    private Long followerId;

    @NotNull(message = "被关注者ID不能为空")
    private Long followingId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    public PortalFollow()
    {
    }

    public PortalFollow(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getFollowerId()
    {
        return followerId;
    }

    public void setFollowerId(Long followerId)
    {
        this.followerId = followerId;
    }

    public Long getFollowingId()
    {
        return followingId;
    }

    public void setFollowingId(Long followingId)
    {
        this.followingId = followingId;
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

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("followerId", getFollowerId())
            .append("followingId", getFollowingId())
            .append("createTime", getCreateTime())
            .toString();
    }
}