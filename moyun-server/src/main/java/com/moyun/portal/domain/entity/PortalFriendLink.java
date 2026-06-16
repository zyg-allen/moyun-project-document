package com.moyun.portal.domain.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

@Data
@TableName("portal_friend_link")
public class PortalFriendLink extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "链接名称不能为空")
    @Size(min = 0, max = 100, message = "链接名称长度不能超过100个字符")
    private String name;

    @NotBlank(message = "链接地址不能为空")
    @Size(min = 0, max = 500, message = "链接地址长度不能超过500个字符")
    private String url;

    @Size(min = 0, max = 500, message = "链接描述长度不能超过500个字符")
    private String description;

    @Size(min = 0, max = 500, message = "Logo URL长度不能超过500个字符")
    private String logo;

    private Integer sort;

    @Size(min = 0, max = 20, message = "状态长度不能超过20个字符")
    private String status;

    public PortalFriendLink() {
    }

    public PortalFriendLink(Long id) {
        this.id = id;
    }
}
