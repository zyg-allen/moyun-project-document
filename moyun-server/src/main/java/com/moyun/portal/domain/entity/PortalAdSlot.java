package com.moyun.portal.domain.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

@Data
@TableName("portal_ad_slot")
public class PortalAdSlot extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "广告位标识不能为空")
    @Size(min = 0, max = 64, message = "广告位标识长度不能超过64个字符")
    private String slotKey;

    @NotBlank(message = "广告标题不能为空")
    @Size(min = 0, max = 100, message = "广告标题长度不能超过100个字符")
    private String title;

    @Size(min = 0, max = 500, message = "广告图片URL长度不能超过500个字符")
    private String image;

    @Size(min = 0, max = 500, message = "点击跳转链接长度不能超过500个字符")
    private String link;

    @Size(min = 0, max = 500, message = "广告文案长度不能超过500个字符")
    private String content;

    private Integer sort;

    @Size(min = 0, max = 1, message = "状态长度不能超过1个字符")
    private String status;

    public PortalAdSlot() {
    }

    public PortalAdSlot(Long id) {
        this.id = id;
    }
}
